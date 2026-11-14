package com.example.feature.culture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings

enum class QuizCategory(val labelEn: String, val labelNe: String) {
    ALL("All Topics", "सबै विषय"),
    LANGUAGES("Languages", "भाषाहरू"),
    NORMS("Culture & Norms", "संस्कृति र परम्परा"),
    VALUES("Moral Values", "नैतिक मूल्य")
}

data class NepaliQuizQuestion(
    val id: String,
    val category: QuizCategory,
    val questionEn: String,
    val questionNe: String,
    val optionsEn: List<String>,
    val optionsNe: List<String>,
    val correctIndex: Int,
    val explanationEn: String,
    val explanationNe: String
)

object NepaliCultureQuizBank {
    val questions: List<NepaliQuizQuestion> = listOf(
        NepaliQuizQuestion(
            id = "q_newari_jwojala",
            category = QuizCategory.LANGUAGES,
            questionEn = "In Nepal Bhasa (Newari), what does the traditional greeting 'Jwojalapa' signify?",
            questionNe = "नेपाल भाषा (नेवारी) मा परम्परागत अभिवादन 'ज्वज्वलपा' को अर्थ के हो?",
            optionsEn = listOf(
                "May your inner flame and spirit shine brightly (Respectful Namaste)",
                "Good harvest in autumn",
                "Welcome to the feast",
                "Safe mountain journey"
            ),
            optionsNe = listOf(
                "तपाईंको आन्तरिक तेज र आत्मा उज्यालो रहोस् (सम्मानजनक नमस्ते)",
                "शरद ऋतुको राम्रो फसल",
                "भोजमा स्वागत छ",
                "सुरक्षित पहाडी यात्रा"
            ),
            correctIndex = 0,
            explanationEn = "'Jwojalapa' combines 'jwala' (flame/light) and 'pa' (joining palms), honoring the divine spark in another person.",
            explanationNe = "'ज्वज्वलपा' ले अर्को व्यक्तिमा रहेको दिव्य ज्योतिलाई सम्मान गर्ने गहिरो नेवारी अभिवादन जनाउँछ।"
        ),
        NepaliQuizQuestion(
            id = "q_tamang_lhasso",
            category = QuizCategory.LANGUAGES,
            questionEn = "Which Tamang greeting expression translates closely to wishing blessings and polite respect?",
            questionNe = "तामाङ भाषामा आशीर्वाद र सम्मानजनक अभिवादनका लागि कुन शब्द प्रयोग गरिन्छ?",
            optionsEn = listOf("Tashi Delek", "Lhasso Fyafulla", "Sewaro", "Sabalai Ramro"),
            optionsNe = listOf("ताशी देलेक", "ल्हास्सो फ्याफुल्ला", "सेवारो", "सबैलाई राम्रो"),
            correctIndex = 1,
            explanationEn = "'Lhasso Fyafulla' is the warm, respectful Tamang greeting offered to elders, guests, and friends.",
            explanationNe = "'ल्हास्सो फ्याफुल्ला' तामाङ समुदायमा पाहुना, अग्रज र मित्रहरूलाई गरिने न्यानो अभिवादन हो।"
        ),
        NepaliQuizQuestion(
            id = "q_limbu_sewaro",
            category = QuizCategory.LANGUAGES,
            questionEn = "Which indigenous Nepali community in eastern Nepal uses 'Sewaro' as their respectful greeting?",
            questionNe = "पूर्वी नेपालको कुन समुदायले सम्मानजनक अभिवादनको रूपमा 'सेवारो' प्रयोग गर्दछन्?",
            optionsEn = listOf("Limbu (Yakthung)", "Tharu", "Gurung (Tamu)", "Magar"),
            optionsNe = listOf("लिम्बू (याक्थुङ)", "थारु", "गुरुङ (तमु)", "मगर"),
            correctIndex = 0,
            explanationEn = "'Sewaro' is the Yakthung (Limbu) greeting meaning respectful homage and goodwill.",
            explanationNe = "'सेवारो' याक्थुङ (लिम्बू) समुदायको आदर र सद्भाव व्यक्त गर्ने परम्परागत अभिवादन हो।"
        ),
        NepaliQuizQuestion(
            id = "q_maithili_pranam",
            category = QuizCategory.LANGUAGES,
            questionEn = "In Maithili culture of Madhesh/Janakpur, how do people greet respectfully and warmly?",
            questionNe = "मधेस र जनकपुरको मैथिली संस्कृतिमा आदरपूर्वक अभिवादन कसरी गरिन्छ?",
            optionsEn = listOf("Pranam / Gor Lagai Chhi", "Khambu", "Dharne", "Chhauni"),
            optionsNe = listOf("प्रणाम / गोर लगै छी", "खम्बु", "धर्ने", "छाउनी"),
            correctIndex = 0,
            explanationEn = "Maithili culture places great value on polite address ('Aahaan' and 'Pranam'), expressing humility and respect.",
            explanationNe = "मैथिली संस्कृतिमा 'अहाँ' र 'प्रणाम' मार्फत विनम्रता र गहिरो आदर प्रकट गरिन्छ।"
        ),
        NepaliQuizQuestion(
            id = "q_norm_parma",
            category = QuizCategory.NORMS,
            questionEn = "What does the traditional Nepali rural practice of 'Parma' represent?",
            questionNe = "नेपाली ग्रामीण समाजको परम्परागत 'पर्म' प्रथाले के जनाउँछ?",
            optionsEn = listOf(
                "Reciprocal community labor exchange during planting & harvest",
                "A winter temple festival",
                "Playing flute in the hills",
                "Pottery painting in Bhaktapur"
            ),
            optionsNe = listOf(
                "रोपाइँ र बाली भित्र्याउँदा गाउँलेबीच पालैपालो श्रम आदानप्रदान",
                "जाडो यामको मन्दिर जात्रा",
                "पहाडमा बाँसुरी बजाउने चलन",
                "भक्तपुरको माटो कला"
            ),
            correctIndex = 0,
            explanationEn = "Parma builds neighborhood solidarity across Nepal—families help one another farm and harvest without cash wages.",
            explanationNe = "पर्मले श्रम र सहयोग साटासाट मार्फत नेपाली समुदायमा एकता र आपसी सद्भाव बलियो बनाउँछ।"
        ),
        NepaliQuizQuestion(
            id = "q_norm_guthi",
            category = QuizCategory.NORMS,
            questionEn = "Which traditional Newar social institution preserves temples, water spouts, music, and communal feasts?",
            questionNe = "मन्दिर, ढुङ्गेधारा, दाफा भजन र सामाजिक भोज संरक्षण गर्ने नेवारी संस्थालाई के भनिन्छ?",
            optionsEn = listOf("Bazar", "Guthi", "Chautari", "Pati"),
            optionsNe = listOf("बजार", "गुठी", "चौतारी", "पाटी"),
            correctIndex = 1,
            explanationEn = "The Guthi system is centuries old, uniting community stewardship with cultural and social welfare.",
            explanationNe = "शताब्दीयौं पुरानो गुठी प्रणालीले सम्पदा, संस्कृति र सामाजिक कल्याणको सामूहिक संरक्षण गर्दछ।"
        ),
        NepaliQuizQuestion(
            id = "q_norm_rodhi",
            category = QuizCategory.NORMS,
            questionEn = "In Gurung (Tamu) culture, what is 'Rodhi' traditionally known for?",
            questionNe = "गुरुङ (तमु) संस्कृतिमा 'रोधी' परम्परागत रूपमा केका लागि परिचित छ?",
            optionsEn = listOf(
                "Evening community gathering of youth for folk songs, dance, and friendship",
                "Stone carving workshop",
                "Rice fermentation vessel",
                "Monsoon river crossing"
            ),
            optionsNe = listOf(
                "युवायुवती भेला भई लोकगीत, नृत्य र मित्रता साट्ने साँझको सांस्कृतिक थलो",
                "ढुङ्गा कुँद्ने कार्यशाला",
                "अन्न राख्ने भाँडो",
                "वर्षामा नदी तर्ने तरिका"
            ),
            correctIndex = 0,
            explanationEn = "Guided by respected elders ('Rodhi Aama'), Rodhi nurtured music, cooperation, and respectful social bonds.",
            explanationNe = "'रोधी आमा' को अभिभावकत्वमा रोधीले लोकगीत, नृत्य र सभ्य सामाजिक सम्बन्ध प्रवर्द्धन गर्दथ्यो।"
        ),
        NepaliQuizQuestion(
            id = "q_norm_samay_baji",
            category = QuizCategory.NORMS,
            questionEn = "Which beaten-rice platter symbolizes blessing and auspicious celebration in Newar feasts?",
            questionNe = "नेवारी भोज र शुभ अवसरमा सगुनको रूपमा खाइने चिउरासहितको विशेष परिकार कुन हो?",
            optionsEn = listOf("Sel Roti", "Samay Baji", "Dhido", "Gundruk"),
            optionsNe = listOf("सेल रोटी", "समयबजी", "ढिँडो", "गुन्द्रुक"),
            correctIndex = 1,
            explanationEn = "Samay Baji features chiura (beaten rice), roasted meat or soybeans, black bhatmas, ginger, and spinach—offering balanced flavors.",
            explanationNe = "समयबजीमा चिउरा, भटमास, अदुवा, छोइला/साग र अचार समेटिन्छ जसले शुभ र सद्भाव जनाउँछ।"
        ),
        NepaliQuizQuestion(
            id = "q_value_atithi",
            category = QuizCategory.VALUES,
            questionEn = "What moral principle guides Nepali hospitality: 'Atithi Devo Bhava'?",
            questionNe = "'अतिथि देवो भव:' भन्ने नेपाली संस्कारले कस्तो नैतिक मूल्य सिकाउँछ?",
            optionsEn = listOf(
                "A guest is treated with reverence as a manifestation of the divine",
                "Guests should only visit during Dashain",
                "Hospitality is limited to family elders",
                "Only neighbors are invited for chiya"
            ),
            optionsNe = listOf(
                "घरमा आएका पाहुनालाई ईश्वर सरह आदर र सत्कार गर्नु",
                "दशैंमा मात्र पाहुना बोलाउनु",
                "केवल जेष्ठ सदस्यलाई मात्र सम्मान गर्नु",
                "छिमेकीलाई मात्र चिया खुवाउनु"
            ),
            correctIndex = 0,
            explanationEn = "Welcoming guests warmly with a smile, chiya, or a meal reflects generosity and humility across all Nepali homes.",
            explanationNe = "मुस्कान, चिया र न्यानो स्वागत सहित पाहुनाको सत्कार गर्नु नेपाली नैतिकताको आधार हो।"
        ),
        NepaliQuizQuestion(
            id = "q_value_dhog",
            category = QuizCategory.VALUES,
            questionEn = "During Dashain, receiving Tika and Jamara from elders teaches what core moral value?",
            questionNe = "दशैंमा मान्यजनको हातबाट टीका र जमरा थाप्दा कुन प्रमुख नैतिक मूल्य झल्किन्छ?",
            optionsEn = listOf(
                "Intergenerational respect, gratitude, and wishing blessings of health & harmony",
                "Competition over gifts",
                "Staying silent all day",
                "Traveling without telling family"
            ),
            optionsNe = listOf(
                "अग्रजप्रतिको सम्मान, कृतज्ञता र सुस्वास्थ्य तथा सद्भावको आशीर्वाद",
                "उपहारको प्रतिस्पर्धा",
                "दिनभर मौन बस्ने नियम",
                "परिवारलाई नसोधी यात्रा गर्ने"
            ),
            correctIndex = 0,
            explanationEn = "Respecting elders and blessing younger generations strengthens family unity and empathy.",
            explanationNe = "मान्यजनको आदर र सानालाई स्नेह तथा आशीर्वाद दिने परम्पराले परिवार र समाजमा आत्मीयता जोगाउँछ।"
        ),
        NepaliQuizQuestion(
            id = "q_value_chautari",
            category = QuizCategory.VALUES,
            questionEn = "Building a 'Chautari' (resting platform with Bar and Pipal trees) along mountain trails reflects what moral virtue?",
            questionNe = "पहाडी बाटोमा वर-पीपल रोपेर चौतारी बनाउने परम्पराले कुन नैतिक गुण दर्शाउँछ?",
            optionsEn = listOf(
                "Selfless public service (Punya) so future travelers find shade and rest",
                "Private land ownership",
                "Commercial toll collection",
                "Festival fireworks storage"
            ),
            optionsNe = listOf(
                "आगामी बटुवाहरूले शीतल छाया र आराम पाऊन् भन्ने निस्वार्थ परोपकार (पुण्य)",
                "निजी जग्गाको सिमाना",
                "कर उठाउने स्थान",
                "पटाका राख्ने ठाउँ"
            ),
            correctIndex = 0,
            explanationEn = "Planting Bar-Pipal and stone benches helps strangers rest centuries later—pure civic compassion.",
            explanationNe = "आफूले नचिन्ने यात्रुले समेत शीतल छायामा विश्राम पाऊन् भनी चौतारी बनाउनु निस्वार्थ सेवा हो।"
        ),
        NepaliQuizQuestion(
            id = "q_language_tharu",
            category = QuizCategory.LANGUAGES,
            questionEn = "Which vibrant festival marks the New Year for the Tharu community of Nepal's Terai?",
            questionNe = "तराईका थारु समुदायले कुन पर्वलाई नयाँ वर्षको रूपमा भव्यताका साथ मनाउँछन्?",
            optionsEn = listOf("Maghi (Maghe Sankranti)", "Indra Jatra", "Mani Rimdu", "Chhath"),
            optionsNe = listOf("माघी (माघे संक्रान्ति)", "इन्द्रजात्रा", "मणि रिम्दु", "छठ"),
            correctIndex = 0,
            explanationEn = "Maghi brings families together in reconciliation, folk dances (Maghauta), and community leadership selection (Badghar).",
            explanationNe = "माघी पर्वमा थारु समुदायले मेलमिलाप, मघौटा नाच र बडघर चयन गर्दै नयाँ वर्ष मनाउँछन्।"
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CultureQuizSheet(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    var selectedCategory by remember { mutableStateOf(QuizCategory.ALL) }
    val activeQuestions = remember(selectedCategory) {
        if (selectedCategory == QuizCategory.ALL) NepaliCultureQuizBank.questions
        else NepaliCultureQuizBank.questions.filter { it.category == selectedCategory }
    }
    var currentIndex by remember(selectedCategory) { mutableIntStateOf(0) }
    var selectedAnswer by remember(selectedCategory, currentIndex) { mutableStateOf<Int?>(null) }
    var score by remember(selectedCategory) { mutableIntStateOf(0) }
    var completed by remember(selectedCategory) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isNe) "नेपाली संस्कृति र नैतिक क्विज" else "Nepali Culture & Language Quiz",
                        style = MeroType.Heading.copy(color = colors.text)
                    )
                }
                MeroIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = s.close,
                    onClick = onDismiss
                )
            }

            // Category Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                QuizCategory.entries.forEach { cat ->
                    MeroChip(
                        label = if (isNe) cat.labelNe else cat.labelEn,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        testTag = "quiz_cat_${cat.name.lowercase()}"
                    )
                }
            }

            if (completed || activeQuestions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(MeroRadius.card),
                    color = colors.surface2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                        modifier = Modifier.padding(MeroSpacing.lg)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isNe) {
                                "बधाई छ! तपाईंको अङ्क: $score / ${activeQuestions.size}"
                            } else {
                                "Well Done! Score: $score / ${activeQuestions.size}"
                            },
                            style = MeroType.Title.copy(color = colors.text),
                            textAlign = TextAlign.Center
                        )
                        val rank = when {
                            activeQuestions.isNotEmpty() && score * 100 / activeQuestions.size >= 80 ->
                                if (isNe) "संस्कृति र सद्भाव दूत" else "Cultural Heritage Ambassador"
                            activeQuestions.isNotEmpty() && score * 100 / activeQuestions.size >= 50 ->
                                if (isNe) "नेपाली परम्परा अन्वेषक" else "Nepali Culture Explorer"
                            else ->
                                if (isNe) "जिज्ञासु यात्री" else "Curious Cultural Learner"
                        }
                        Text(
                            text = rank,
                            style = MeroType.Body.copy(color = colors.primary, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        MeroButton(
                            text = if (isNe) "फेरि खेल्नुहोस्" else "Play Again",
                            leadingIcon = Icons.Filled.Refresh,
                            onClick = {
                                currentIndex = 0
                                score = 0
                                selectedAnswer = null
                                completed = false
                            },
                            testTag = "quiz_play_again"
                        )
                        MeroSecondaryButton(
                            text = s.close,
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                val q = activeQuestions[currentIndex.coerceIn(0, activeQuestions.lastIndex)]
                val options = if (isNe) q.optionsNe else q.optionsEn

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isNe) {
                            "प्रश्न ${currentIndex + 1} / ${activeQuestions.size}"
                        } else {
                            "Question ${currentIndex + 1} of ${activeQuestions.size}"
                        },
                        style = MeroType.Caption.copy(color = colors.textMuted, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (isNe) "अङ्क: $score" else "Score: $score",
                        style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / activeQuestions.size.toFloat() },
                    color = colors.primary,
                    trackColor = colors.surface2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isNe) q.questionNe else q.questionEn,
                    style = MeroType.Heading.copy(color = colors.text)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEachIndexed { idx, optText ->
                        val answered = selectedAnswer != null
                        val isCorrect = idx == q.correctIndex
                        val isPicked = selectedAnswer == idx
                        val bgColor = when {
                            !answered -> colors.surface2
                            isCorrect -> colors.success.copy(alpha = 0.16f)
                            isPicked && !isCorrect -> colors.danger.copy(alpha = 0.16f)
                            else -> colors.surface2
                        }
                        val borderColor = when {
                            !answered -> colors.border
                            isCorrect -> colors.success
                            isPicked && !isCorrect -> colors.danger
                            else -> colors.border
                        }

                        Surface(
                            shape = RoundedCornerShape(MeroRadius.small),
                            color = bgColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(MeroRadius.small))
                                .clickable(enabled = !answered) {
                                    selectedAnswer = idx
                                    if (idx == q.correctIndex) score += 1
                                }
                                .testTag("quiz_option_$idx")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.md)
                            ) {
                                Text(
                                    text = optText,
                                    style = MeroType.Body.copy(
                                        color = colors.text,
                                        fontWeight = if (answered && isCorrect) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (answered && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedAnswer != null) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.small),
                        color = colors.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isNe) q.explanationNe else q.explanationEn,
                            style = MeroType.Caption.copy(color = colors.text),
                            modifier = Modifier.padding(MeroSpacing.md)
                        )
                    }

                    MeroButton(
                        text = if (currentIndex == activeQuestions.lastIndex) {
                            if (isNe) "नतिजा हेर्नुहोस्" else "Finish Quiz"
                        } else {
                            s.next
                        },
                        onClick = {
                            if (currentIndex == activeQuestions.lastIndex) {
                                completed = true
                            } else {
                                currentIndex += 1
                                selectedAnswer = null
                            }
                        },
                        testTag = "quiz_next_button"
                    )
                }
            }
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}
