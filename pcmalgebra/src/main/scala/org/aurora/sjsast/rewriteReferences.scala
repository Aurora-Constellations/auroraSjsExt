package org.aurora.sjsast

object rewriteReferences:
    def rewriteOrdersReferences(orders: Orders, originalName: String, aliasName: String): Orders =
        val rewrittenNGOs = orders.ngo.map { ngo =>
            val rewrittenCoords = ngo.orderCoordinates.map { oc =>
            val rewrittenRefs = rewriteQuReferences(oc.refs, originalName, aliasName)
            oc.copy(refs = rewrittenRefs)
            }
            val rewrittenQuRefs = rewriteQuReferences(ngo.quRefs, originalName, aliasName)
            ngo.copy(orderCoordinates = rewrittenCoords, quRefs = rewrittenQuRefs)
        }
        orders.copy(ngo = rewrittenNGOs)

    def rewriteClinicalReferences(clinical: Clinical, originalName: String, aliasName: String): Clinical =
        val rewrittenNGCs = clinical.ngc.map { ngc =>
            val rewrittenCoords = ngc.ccoords.map { ccv =>
            val rewrittenRefs = ccv.refs.map { rc =>
                if rc.name == originalName then rc.copy(name = aliasName) else rc
            }
            ccv.copy(refs = rewrittenRefs)
            }
            val rewrittenQuRefs = rewriteQuReferences(ngc.quRefs, originalName, aliasName)
            ngc.copy(ccoords = rewrittenCoords, quRefs = rewrittenQuRefs)
        }
        clinical.copy(ngc = rewrittenNGCs)

    def rewriteQuReferences(qrs: QuReferences, originalName: String, aliasName: String): QuReferences =
        val rewrittenRefs = qrs.refs.map { qr =>
            if qr.name == originalName then qr.copy(name = aliasName) else qr
        }
        qrs.copy(refs = rewrittenRefs)