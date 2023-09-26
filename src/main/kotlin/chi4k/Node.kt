package chi4k

import org.eclipse.jetty.http.HttpParser.HttpHandler

class Node(
    var subroutes: Routes? = null,
    var rex: Regex? = null,
    var endpoints: MutableMap<MethodTyp, Endpoint> = mutableMapOf<MethodTyp, Endpoint>(),
    var prefix: String = "",
    var children: Map<NodeTyp, MutableList<Node>> = NodeTyp.entries.associateWith { mutableListOf() },
    var tail: Char = Char(0),
    var typ: NodeTyp = NodeTyp.NT_STATIC,
    var label: Char = Char(0),
) {
    fun value(method: MethodTyp): Endpoint {
        return endpoints.getOrPut(method) { Endpoint() }
    }

    fun insertRoute(method: MethodTyp, pattern: String, handler: HttpHandler): Node {
        var parent: Node? = null
        var search = pattern

        while (true) {
            // Handle key exhaustion
            if (search.isEmpty()) {
                // Insert or update the node's leaf handler
                this.setEndpoint(method, handler, pattern)
                return this
            }

            // We're going to be searching for a wild node next
            val label = search[0]
            val (segTyp, _, segRexpat, segTail, _, segEndIdx) = if (label == '{' || label == '*') {
                patNextSegment(search)
            } else {
                SegmentDetails(NodeTyp.NT_STATIC, "", "", Char(0), 0, 0)
            }

            val prefix = if (segTyp == NodeTyp.NT_REGEXP) segRexpat else ""

            // Look for the edge to attach to
            parent = this
            val n = this.getEdge(segTyp, label, segTail, prefix)

            // No edge, create one
            if (n == null) {
                val child = Node(label = label, tail = segTail, prefix = search)
                val hn = parent.addChild(child, search)
                hn.setEndpoint(method, handler, pattern)
                return hn
            }

            // Found an edge to match the pattern
            if (n.typ > NodeTyp.NT_STATIC) {
                // We found a param node, trim the param from the search path and continue.
                search = search.substring(segEndIdx)
                continue
            }

            // Static nodes fall below here.
            val commonPrefix = longestPrefix(search, n.prefix)
            if (commonPrefix == n.prefix.length) {
                // the common prefix is as long as the current node's prefix we're attempting to insert.
                search = search.substring(commonPrefix)
                continue
            }

            // Split the node
            val child = Node(typ = NodeTyp.NT_STATIC, prefix = search.substring(0, commonPrefix))
            parent.replaceChild(search[0], segTail, child)

            // Restore the existing node
            n.label = n.prefix[commonPrefix]
            n.prefix = n.prefix.substring(commonPrefix)
            child.addChild(n, n.prefix)

            // If the new key is a subset, set the method/handler on this node and finish.
            search = search.substring(commonPrefix)
            if (search.isEmpty()) {
                child.setEndpoint(method, handler, pattern)
                return child
            }

            // Create a new edge for the node
            val subchild = Node(typ = NodeTyp.NT_STATIC, label = search[0], prefix = search)
            val hn = child.addChild(subchild, search)
            hn.setEndpoint(method, handler, pattern)
            return hn
        }
    }


    fun addChild(child: Node, prefix: String): Node {
        var search = prefix
        var hn = child

        // Parse next segment
        val (segTyp, _, segRexpat, segTail, segStartIdx, segEndIdx) = patNextSegment(search)

        // Add child depending on next up segment
        when (segTyp) {
            NodeTyp.NT_STATIC -> {
                // noop
            }

            else -> {
                if (segTyp == NodeTyp.NT_REGEXP) {
                    val rex = Regex(segRexpat)
                    child.prefix = segRexpat
                    child.rex = rex
                }

                when {
                    segStartIdx == 0 -> {
                        child.typ = segTyp
                        if (segTyp != NodeTyp.NT_CATCH_ALL) {
                            search = search.substring(segEndIdx)
                        } else {
                            search = ""
                        }
                        child.tail = segTail

                        if (search.isNotEmpty()) {
                            val nn = Node(typ = NodeTyp.NT_STATIC, label = search[0], prefix = search)
                            hn = child.addChild(nn, search)
                        }
                    }

                    else -> {
                        child.typ = NodeTyp.NT_STATIC
                        child.prefix = search.substring(0, segStartIdx)
                        child.rex = null

                        search = search.substring(segStartIdx)
                        val nn = Node(typ = segTyp, label = search[0], tail = segTail)
                        hn = child.addChild(nn, search)
                    }
                }
            }
        }

        this.children[segTyp]?.add(child)
        this.children[segTyp]?.xSort() // Assumes nodes have a natural order
        return hn
    }


    fun replaceChild(label: Char, tail: Char, child: Node) {
        for (i in children[child.typ]!!.indices) {
            if (children[child.typ]!![i].label == label && children[child.typ]!![i].tail == tail) {
                children[child.typ]!![i] = child
                children[child.typ]!![i].label = label
                children[child.typ]!![i].tail = tail
                return
            }
        }
        throw Exception("chi: replacing missing child")
    }

    fun getEdge(ntyp: NodeTyp, label: Char, tail: Char, prefix: String): Node? {
        val nds = children[ntyp]!!
        for (node in nds) {
            if (node.label == label && node.tail == tail) {
                if (ntyp == NodeTyp.NT_REGEXP && node.prefix != prefix) {
                    continue
                }
                return node
            }
        }
        return null
    }


    fun setEndpoint(method: MethodTyp, handler: HttpHandler, pattern: String) {
        val paramKeys = patParamKeys(pattern) // Assuming function exists

        if (method == MethodTyp.mSTUB) {
            endpoints.value(MethodTyp.mSTUB).handler = handler // Assuming value function exists in Endpoints
        }
//        if (true) {
        val h = endpoints.value(MethodTyp.mALL)
        h.handler = handler
        h.pattern = pattern
        h.paramKeys = paramKeys
        for (m in methodMap.values) { // Assuming methodMap exists
            val h1 = endpoints.value(m)
            h1.handler = handler
            h1.pattern = pattern
            h1.paramKeys = paramKeys
        }
//        } else {
//            val h = endpoints.value(method)
//            h.handler = handler
//            h.pattern = pattern
//            h.paramKeys = paramKeys
    }

    fun findRoute(
        rctx: Context,
        method: MethodTyp,
        path: String
    ): Triple<Node?, MutableMap<MethodTyp, Endpoint>?, HttpHandler?> {
        // Assuming a class called Context exists and it has the properties shown in the function
        rctx.routePattern = ""
        rctx.routeParams.keys.clear()
        rctx.routeParams.values.clear()

        val rn = findRouteImpl(rctx, method, path)
        rn ?: return Triple(null, null, null)

        rctx.URLParams.keys.addAll(rctx.routeParams.keys)
        rctx.URLParams.values.addAll(rctx.routeParams.values)

        if (rn.endpoints[method]?.pattern != "") {
            rctx.routePattern = rn.endpoints[method]!!.pattern
            rctx.routePatterns.add(rctx.routePattern) // Assuming routePatterns is a list
        }

        return Triple(rn, rn.endpoints, rn.endpoints[method]?.handler)
    }

    private fun Node.findRouteImpl(rctx: Context, method: MethodTyp, path: String): Node? {
        var nn = this
        var search = path

        for ((ntyp, nds) in nn.children) {
            if (nds.isEmpty()) continue

            var xn: Node? = null
            var xsearch = search
            val label = xsearch.firstOrNull() ?: Char(0)

            when (ntyp) {
                NodeTyp.NT_STATIC -> {
                    xn = nodesFindEdge(nds, label)
                    if (xn == null || !xsearch.startsWith(xn.prefix)) continue
                    xsearch = xsearch.drop(xn.prefix.length)
                }

                NodeTyp.NT_PARAM, NodeTyp.NT_REGEXP -> {
                    if (xsearch.isEmpty()) continue
                    for (node in nds) {
                        xn = node
                        var p = xsearch.indexOf(xn.tail)
                        if (p < 0) {
                            if (xn.tail == '/') {
                                p = xsearch.length
                            } else {
                                continue
                            }
                        } else if (ntyp == NodeTyp.NT_REGEXP && p == 0) {
                            continue
                        }

                        if (ntyp == NodeTyp.NT_REGEXP && xn.rex != null) {
                            if (xn.rex?.matches(xsearch.substring(0, p)) == false) {
                                continue
                            }
                        } else if (xsearch.substring(0, p).contains('/')) {
                            continue
                        }

                        val prevLen = rctx.routeParams.values.size
                        rctx.routeParams.values.add(xsearch.substring(0, p))
                        xsearch = xsearch.drop(p)
                        if (xsearch.isEmpty()) {
                            if (xn.children.isEmpty()) {
                                val h = xn.endpoints[method]
                                if (h?.handler != null) {
                                    rctx.routeParams.keys.addAll(h.paramKeys)
                                    return xn
                                }

                                for (endpoint in xn.endpoints.keys) {
                                    if ( endpoint == MethodTyp.mSTUB) {
                                        continue
                                    }
                                    rctx.methodsAllowed.add(endpoint)
                                    TODO()
                                }

                                // Flag that the routing context found a route, but not a corresponding supported method
                                rctx.methodNotAllowed = true
                            }
                        }

                        // Recursively find the next node on this branch
                        val (fin, _, _) = xn.findRoute(rctx, method, xsearch)
                        if (fin != null) {
                            return fin
                        }

                        // Not found on this branch, reset vars
                        rctx.routeParams.values = rctx.routeParams.values.subList(0, prevLen).toMutableList()
                        xsearch = search
                    }
                    rctx.routeParams.values.add("")
                }
                // ... (continuation for other cases)
                NodeTyp.NT_CATCH_ALL -> {
                    rctx.routeParams.values += search
                    xn = nds[0]
                    xsearch = ""
                }
            }

            val xn1 = xn ?: continue

            // Did we find it yet?
            if (xsearch.isEmpty()) {
                if (xn1.children.isNotEmpty()) {
                    val h = xn1.endpoints[method]
                    if (h?.handler != null) {
                        rctx.routeParams.keys.addAll(h.paramKeys)
                        return xn
                    }

                    for (endpoint in xn1.endpoints.keys) {
                        if (endpoint == MethodTyp.mSTUB) continue
                        rctx.methodsAllowed.add(endpoint)
                        TODO()
                    }

                    // Flag that the routing context found a route, but not a corresponding supported method
                    rctx.methodNotAllowed = true
                }
            }

            // Recursively find the next node
            val (fin, _, _) = xn1.findRoute(rctx, method, xsearch)
            if (fin != null) {
                return fin
            }

            // Didn't find the final handler, remove the param here if it was set
            if (xn1.typ > NodeTyp.NT_STATIC) {
                if (rctx.routeParams.values.isNotEmpty()) {
                    rctx.routeParams.values.removeAt(rctx.routeParams.values.size - 1)
                }
            }
        }

        return null
    }


    fun findEdge(ntyp: NodeTyp, label: Char): Node? {
        val nds = children[ntyp] ?: return null
        val num = nds.size
        var idx = 0

        when (ntyp) {
            NodeTyp.NT_STATIC, NodeTyp.NT_PARAM, NodeTyp.NT_REGEXP -> {
                var i = 0
                var j = num - 1
                while (i <= j) {
                    idx = i + (j - i) / 2
                    when {
                        label > nds[idx].label -> i = idx + 1
                        label < nds[idx].label -> j = idx - 1
                        else -> i = num // breaks condition
                    }
                }
                if (nds[idx].label != label) {
                    return null
                }
            }

            else -> return nds[idx]
        }
        return nds[idx]
    }

    fun findPattern(pattern: String): Boolean {
        val nn: Node = this
        for (nds in nn.children.values) {
            if (nds.isEmpty()) continue

            val n = nn.findEdge(nds[0].typ, pattern[0]) ?: continue
            var idx: Int

            when (n.typ) {
                NodeTyp.NT_STATIC -> {
                    idx = longestPrefix(pattern, n.prefix)
                    if (idx < n.prefix.length) {
                        continue
                    }
                }

                NodeTyp.NT_PARAM, NodeTyp.NT_REGEXP -> idx = pattern.indexOf('}') + 1
                NodeTyp.NT_CATCH_ALL -> idx = longestPrefix(pattern, "*")
                else -> throw Exception("chi: unknown node type")
            }

            val xpattern: String = pattern.substring(idx)
            if (xpattern.isEmpty()) {
                return true
            }

            if (n.findPattern(xpattern)) {
                return true
            }
        }
        return false
    }

    fun routes(): List<Route> {
        val rts = mutableListOf<Route>()

        walk { eps: Map<MethodTyp, Endpoint>, subroutes: Routes? ->
            eps[MethodTyp.mSTUB]?.handler?.let {
                if (subroutes == null) return@walk false
            }

            // Group methodHandlers by unique patterns
            val pats = mutableMapOf<String, MutableMap<MethodTyp, Endpoint>>()

            for ((mt, h) in eps) {
                if (h.pattern == "") continue
                val p = pats.getOrPut(h.pattern) { mutableMapOf() }
                p[mt] = h
            }

            for ((p, mh) in pats) {
                val hs = mutableMapOf<String, HttpHandler>()
                mh[MethodTyp.mALL]?.handler?.let {
                    hs["*"] = it
                }

                for ((mt, h) in mh) {
                    h.handler?.let { handler ->
                        val m = methodTypString(mt)
                        if (m != "") {
                            hs[m] = handler
                        }
                    }
                }

                val rt = Route(subroutes, hs, p)
                rts.add(rt)
            }

            false
        }

        return rts
    }


    fun walk(fn: (eps: MutableMap<MethodTyp, Endpoint>, subroutes: Routes) -> Boolean): Boolean {
        if ((endpoints.isNotEmpty() || subroutes != null) && fn(endpoints!!, subroutes!!)) {
            return true
        }

        for (ns in children.values) {
            for (cn in ns) {
                if (cn.walk(fn)) return true
            }
        }
        return false
    }

}

