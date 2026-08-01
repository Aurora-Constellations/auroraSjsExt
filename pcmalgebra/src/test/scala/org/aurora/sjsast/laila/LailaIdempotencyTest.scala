package org.aurora.sjsast.laila

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.aurora.sjsast.JoinMeet.*
import org.aurora.sjsast.* 
import org.scalatest.compatible.Assertion

class LailaIdempotencyTest extends AnyWordSpec with Matchers:

    /* Idempotency is the idea that no matter how many times you combine the same PCMs, 
       you should be getting the same result as if you had applied the merge operation only once. */ 

    def attemptToDoubleMerge(pcm1: PCM, pcm2: PCM): Assertion = {
        val doubleMergeAttempt = for {
                                        firstMerge  <- Option(pcm1 |+| pcm2)
                                        secondMerge <- Option(firstMerge |+| pcm2)
                                        check       <- Option(firstMerge should be (secondMerge))
                                     } yield check
        doubleMergeAttempt match {
                case Some(value) => value should be (succeed)
                case _ => fail() 
            }
    }

    "Clinicals" should {
        "be idempotent" in {
            val narrative1 = NL_STATEMENT(name="narr1")
            val narrative2 = NL_STATEMENT(name="narr2")
            // val qu = QU(query=LHSet('!'))
            // val quReference = QuReference(qu=qu, refName=???)
            
            // Replaced ClinicalCoordinate with ClinicalItem
            val clinicalItem1 = ClinicalItem(name="cc1", narratives=LHSet(narrative1), qu=QU(query=LHSet()), qurefs=LHSet())
            val clinicalItem2 = ClinicalItem(name="cc2", narratives=LHSet(narrative2), qu=QU(query=LHSet()), qurefs=LHSet())
            val namedGroupClinical1 = NGC(name="ngc1", narratives=LHSet(), coordinates=LHSet(clinicalItem1), refs=LHSet())
            val namedGroupClinical2 = NGC(name="ngc2", narratives=LHSet(), coordinates=LHSet(clinicalItem2), refs=LHSet())

            val clinical1 = Clinical(ngc=LHSet(namedGroupClinical1))
            val clinical2 = Clinical(ngc=LHSet(namedGroupClinical2))

            val pcm1 = PCM(LHMap("Clinical" -> clinical1))
            val pcm2 = PCM(LHMap("Clinical" -> clinical2))

            attemptToDoubleMerge(pcm1, pcm2)            
        }
    }

    "Issues" should {
        "be idempotent" in {
            val narrative1 = NL_STATEMENT(name="narr1")
            val narrative2 = NL_STATEMENT(name="narr2")
            // val qu = QU(query=LHSet('!'))
            // val quReference = QuReference(qu=qu, refName=???)
            val issueCoord1 = IssueCoordinate(name="ic1", narratives=LHSet(narrative1), qu=QU(query=LHSet()), qurefs=LHSet())
            val issueCoord2 = IssueCoordinate(name="ic2", narratives=LHSet(narrative2), qu=QU(query=LHSet()), qurefs=LHSet())
            
            val issues1 = Issues(narratives=LHSet(), ic=LHSet(issueCoord1))
            val issues2 = Issues(narratives=LHSet(), ic=LHSet(issueCoord2))

            val pcm1 = PCM(LHMap("Issues" -> issues1))
            val pcm2 = PCM(LHMap("Issues" -> issues2))

            attemptToDoubleMerge(pcm1, pcm2)            
        }
    }

    "NamedGroupOrers" should {
        "be idempotent" in {
            val narrative1 = NL_STATEMENT(name="narr1")
            val narrative2 = NL_STATEMENT(name="narr2")
            // val qu = QU(query=LHSet('!'))
            // val quReference = QuReference(qu=qu, refName=???)
            val orderCoord1 = OrderCoordinate(name="oc1", narratives=LHSet(narrative1), qurefs=LHSet())
            val orderCoord2 = OrderCoordinate(name="oc2", narratives=LHSet(narrative2), qurefs=LHSet())
            val namedGroupOrder1 = NGO(name="ngo1", narratives=LHSet(), ordercoord=LHSet(orderCoord1), qurefs=LHSet(), qu=LHSet())
            val namedGroupOrder2 = NGO(name="ngo2", narratives=LHSet(), ordercoord=LHSet(orderCoord2), qurefs=LHSet(), qu=LHSet())

            val orders1 = Orders(ngo=LHSet(namedGroupOrder1))
            val orders2 = Orders(ngo=LHSet(namedGroupOrder2))

            val pcm1 = PCM(LHMap("Orders" -> orders1))
            val pcm2 = PCM(LHMap("Orders" -> orders2))

            attemptToDoubleMerge(pcm1, pcm2)            
        }
    }

end LailaIdempotencyTest