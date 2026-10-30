package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class AdaptiveRulesTest {
 @Test void verifiedMasteryAvoidsRepetition(){assertEquals("MASTERED",AdaptiveRules.action(true,true,false,false,true,false,true));}
 @Test void pedagogyCanRequireStudyDespiteMastery(){assertEquals("STUDY",AdaptiveRules.action(true,false,true,false,false,false,true));}
 @Test void prerequisitesPrecedeRemediation(){assertEquals("PREREQUISITE",AdaptiveRules.action(false,true,false,true,true,false,true));}
 @Test void failedAssessmentRequestsReview(){assertEquals("REVIEW",AdaptiveRules.action(false,true,true,true,true,false,true));}
 @Test void exhaustedAttemptsRequestHumanSupport(){assertEquals("TEACHER_SUPPORT",AdaptiveRules.action(false,true,true,true,true,false,false));}
 @Test void completedStudyUnlocksAssessment(){assertEquals("ASSESS",AdaptiveRules.action(false,true,true,true,false,false,true));}
 @Test void completedModuleDoesNotRequireAnotherAttempt(){assertEquals("COMPLETED",AdaptiveRules.action(false,false,true,true,false,true,false));}
}
