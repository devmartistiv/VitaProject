package com.martist.vitamove.core.domain.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


public class ProfanityFilter {


    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(

            "гейнер", "гейнера", "протеин",


            "херес", "хрен", "хреновина", "хреновуха",
            "сукрало", "сукралоза",
            "сукровица",


            "педиатр", "педиатрия", "ортопед", "логопед",


            "херсон", "хершель",


            "трахея", "трахеит",


            "идеал", "идеально", "идеализм"
    ));


    private static final Set<String> PROFANITY_LIST = new HashSet<>(Arrays.asList(

            "бля", "блят", "блядь", "блядск", "блядин", "бляд",
            "хуй", "хуя", "хуи", "хуев", "хуёв", "хер", "херн", "херов",
            "пизд", "пизж", "пизде", "пиздец", "пиздоб", "пиздюк", "пизданут",
            "ебат", "ебал", "ебан", "ебуч", "ебл", "ебош", "ебис", "ебну", "ебена",
            "ёб", "ёбн", "ёбан", "ёбыв", "заёб", "доёб", "уёб", "наёб", "проёб", "въеб", "отъеб",
            "еблан", "ебланск", "ебанат", "ебанутый", "ебануться",


            "сука", "суки", "сучк", "сучар", "сукин", "суходроч",
            "говн", "говнюк", "говнища", "говноед",
            "срать", "срал", "сраный", "серун", "посрать", "насрать",
            "дерьм", "дермо", "дерьмищ",
            "жоп", "жопник", "жополиз", "жопоголов",


            "пид", "пидор", "пидар", "пидрил", "пидорас", "педик", "педрил",
            "петух", "петушар", "петушня", "пидрюг",
            "гомик", "гомос", "педовк", "пидрюга",


            "мудак", "мудил", "мудозвон", "мудачь", "мудачин", "мудень",
            "долбоёб", "долбаёб", "дебил", "дебилоид", "дебилизм",
            "идиот", "идиотизм", "идиотин", "придурок", "придурк",
            "урод", "уродин", "уродлив", "уёбищ", "уебан", "уебок",
            "мраз", "мразот", "тварь", "тварин", "гнид", "гнида",
            "лох", "лошар", "лохушк", "лохан", "лоханулся",


            "залуп", "залупа", "залупень", "залупить",
            "конч", "кончать", "кончен", "кончаю",
            "дроч", "дрочить", "дрочк", "дрочила",
            "трах", "трахат", "трахнут", "трахалк",
            "минет", "минетчик", "отсос", "сосать",
            "шлюх", "шлюшк", "шалав", "блядун", "блядов",
            "курв", "курват", "проститут",


            "хер", "хрен", "член", "писюн", "елда", "елдак",
            "пися", "письк", "вагин", "пилотк",
            "сиськ", "сись", "титьк", "дойк",


            "хуесос", "хуеплет", "хуета", "хуйня", "хуевин", "хуедрыг",
            "пиздабол", "пиздопляс", "пиздострадан",
            "ебланище", "ебалай", "ебальник",
            "охуе", "охуенн", "охуител", "охуяб", "охуяч",
            "опизде", "припизд", "распизд", "опездол",
            "манд", "мандав", "мандюк",
            "муди", "мудил", "мудень", "мудозвон",


            "выебыва", "доебыва", "заебыва", "наебыва", "отъебыва",
            "распиздяй", "проебан", "заебал", "доебал", "наебал",


            "fuck", "fucker", "fucking", "motherfucker", "fck", "fuk",
            "shit", "shitty", "bullshit", "horseshit", "shitter",
            "bitch", "bitching", "ass", "asshole", "arse",
            "dick", "dickhead", "cock", "cocksucker",
            "pussy", "cunt", "twat", "prick",
            "bastard", "damn", "dammit", "hell",
            "whore", "slut", "slutty", "fag", "faggot",
            "nigger", "nigga", "retard", "retarded"
    ));


    private static final Pattern[] EVASION_PATTERNS = {

            Pattern.compile("[xхx][уyu][йиeёij]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[xх][уy][й1иeё!]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[бb6][лl1][яyа@]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[бb6][лl1]я[дd]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[пp][иi1!][зz3][дd]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[пp][иi1][зz3][дd][аa@]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[еe][бb6][аa@лl]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[еeё][бb6][уy]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[сc][уy][кk]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[сc][уy][чc4][кk]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[мm][уy][дd]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[гg][оo0][вv][нh]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[жzж][оo0][пp]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[xхx][еe][рpr]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[дd][рpr][оo0][чc4]", Pattern.CASE_INSENSITIVE),


            Pattern.compile("f[u\\*u]+[c\\*c]+k", Pattern.CASE_INSENSITIVE),
            Pattern.compile("sh[i\\*i1!]+t", Pattern.CASE_INSENSITIVE),
            Pattern.compile("b[i1!]+tch", Pattern.CASE_INSENSITIVE),
            Pattern.compile("d[i1!]+ck", Pattern.CASE_INSENSITIVE),
            Pattern.compile("a[s\\$5]+", Pattern.CASE_INSENSITIVE),


            Pattern.compile("х\\s*у\\s*й", Pattern.CASE_INSENSITIVE),
            Pattern.compile("б\\s*л\\s*я", Pattern.CASE_INSENSITIVE),
            Pattern.compile("п\\s*и\\s*з\\s*д", Pattern.CASE_INSENSITIVE),
            Pattern.compile("е\\s*б\\s*а", Pattern.CASE_INSENSITIVE)
    };


    public static boolean containsProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }


        String normalizedText = text.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();


        for (String whitelisted : WHITELIST) {
            if (normalizedText.contains(whitelisted)) {

                return false;
            }
        }


        for (String profanity : PROFANITY_LIST) {


            if (normalizedText.contains(profanity)) {
                return true;
            }
        }


        for (Pattern pattern : EVASION_PATTERNS) {
            if (pattern.matcher(normalizedText).find()) {
                return true;
            }
        }

        return false;
    }


    public static String getFirstProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String normalizedText = text.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();


        for (String profanity : PROFANITY_LIST) {
            if (normalizedText.contains(profanity)) {
                return profanity;
            }
        }


        for (Pattern pattern : EVASION_PATTERNS) {
            if (pattern.matcher(normalizedText).find()) {
                return "замаскированное слово";
            }
        }

        return null;
    }


    public static String censorText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String result = text;

        for (String profanity : PROFANITY_LIST) {
            String replacement = profanity.charAt(0) + "***";
            result = result.replaceAll("(?i)\\b" + Pattern.quote(profanity) + "\\b", replacement);
            if (profanity.length() >= 3) {
                result = result.replaceAll("(?i)" + Pattern.quote(profanity), replacement);
            }
        }

        return result;
    }
}
