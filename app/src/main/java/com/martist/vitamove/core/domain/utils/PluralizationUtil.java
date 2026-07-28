package com.martist.vitamove.core.domain.utils;

import java.util.HashMap;
import java.util.Map;


public class PluralizationUtil {


    private static final Map<String, String[]> pluralForms = new HashMap<>();

    static {

        pluralForms.put("банка", new String[]{"банки", "банок"});
        pluralForms.put("баночка", new String[]{"баночки", "баночек"});
        pluralForms.put("батончик", new String[]{"батончика", "батончиков"});
        pluralForms.put("блин", new String[]{"блина", "блинов"});
        pluralForms.put("блинчик", new String[]{"блинчика", "блинчиков"});
        pluralForms.put("брикет", new String[]{"брикета", "брикетов"});
        pluralForms.put("булочка", new String[]{"булочки", "булочек"});
        pluralForms.put("бургер", new String[]{"бургера", "бургеров"});
        pluralForms.put("бутерброд", new String[]{"бутерброда", "бутербродов"});
        pluralForms.put("бутылка", new String[]{"бутылки", "бутылок"});
        pluralForms.put("бутылочка", new String[]{"бутылочки", "бутылочек"});
        pluralForms.put("вафля", new String[]{"вафли", "вафель"});
        pluralForms.put("веточка", new String[]{"веточки", "веточек"});
        pluralForms.put("гамбургер", new String[]{"гамбургера", "гамбургеров"});
        pluralForms.put("голубец", new String[]{"голубца", "голубцов"});
        pluralForms.put("горсть", new String[]{"горсти", "горстей"});
        pluralForms.put("грамм", new String[]{"грамма", "граммов"});
        pluralForms.put("гриб", new String[]{"гриба", "грибов"});
        pluralForms.put("грудка", new String[]{"грудки", "грудок"});
        pluralForms.put("груша", new String[]{"груши", "груш"});
        pluralForms.put("долька", new String[]{"дольки", "долек"});
        pluralForms.put("зубчик", new String[]{"зубчика", "зубчиков"});
        pluralForms.put("колечко", new String[]{"колечка", "колечек"});
        pluralForms.put("конфета", new String[]{"конфеты", "конфет"});
        pluralForms.put("котлета", new String[]{"котлеты", "котлет"});
        pluralForms.put("кочанчик", new String[]{"кочанчика", "кочанчиков"});
        pluralForms.put("креветка", new String[]{"креветки", "креветок"});
        pluralForms.put("круассан", new String[]{"круассана", "круассанов"});
        pluralForms.put("кружка", new String[]{"кружки", "кружек"});
        pluralForms.put("кусок", new String[]{"куска", "кусков"});
        pluralForms.put("кусочек", new String[]{"кусочка", "кусочков"});
        pluralForms.put("лист", new String[]{"листа", "листов"});
        pluralForms.put("листик", new String[]{"листика", "листиков"});
        pluralForms.put("лимон", new String[]{"лимона", "лимонов"});
        pluralForms.put("ломтик", new String[]{"ломтика", "ломтиков"});
        pluralForms.put("ложка", new String[]{"ложки", "ложек"});
        pluralForms.put("луковица", new String[]{"луковицы", "луковиц"});
        pluralForms.put("мант", new String[]{"манта", "мантов"});
        pluralForms.put("маслина", new String[]{"маслины", "маслин"});
        pluralForms.put("миллилитр", new String[]{"миллилитра", "миллилитров"});
        pluralForms.put("орех", new String[]{"ореха", "орехов"});
        pluralForms.put("пакет", new String[]{"пакета", "пакетов"});
        pluralForms.put("палочка", new String[]{"палочки", "палочек"});
        pluralForms.put("пачка", new String[]{"пачки", "пачек"});
        pluralForms.put("пельмень", new String[]{"пельменя", "пельменей"});
        pluralForms.put("перо", new String[]{"пера", "перьев"});
        pluralForms.put("печенье", new String[]{"печенья", "печений"});
        pluralForms.put("пицца", new String[]{"пиццы", "пицц"});
        pluralForms.put("плитка", new String[]{"плитки", "плиток"});
        pluralForms.put("половина", new String[]{"половины", "половин"});
        pluralForms.put("половинка", new String[]{"половинки", "половинок"});
        pluralForms.put("полоска", new String[]{"полоски", "полосок"});
        pluralForms.put("порция", new String[]{"порции", "порций"});
        pluralForms.put("початок", new String[]{"початка", "початков"});
        pluralForms.put("продукт", new String[]{"продукта", "продуктов"});
        pluralForms.put("пучок", new String[]{"пучка", "пучков"});
        pluralForms.put("ролл", new String[]{"ролла", "роллов"});
        pluralForms.put("сарделька", new String[]{"сардельки", "сарделек"});
        pluralForms.put("сосиска", new String[]{"сосиски", "сосисок"});
        pluralForms.put("соцветие", new String[]{"соцветия", "соцветий"});
        pluralForms.put("стакан", new String[]{"стакана", "стаканов"});
        pluralForms.put("стаканчик", new String[]{"стаканчика", "стаканчиков"});
        pluralForms.put("чашка", new String[]{"чашки", "чашек"});
        pluralForms.put("штука", new String[]{"штуки", "штук"});
        pluralForms.put("яйцо", new String[]{"яйца", "яиц"});


        pluralForms.put("1-2 штуки", new String[]{"1-2 штуки", "1-2 штук"});
        pluralForms.put("2-3 штуки", new String[]{"2-3 штуки", "2-3 штук"});


        pluralForms.put("большая банка", new String[]{"большие банки", "больших банок"});
        pluralForms.put("большая пачка", new String[]{"большие пачки", "больших пачек"});
        pluralForms.put("большая порция", new String[]{"большие порции", "больших порций"});
        pluralForms.put("большая чашка", new String[]{"большие чашки", "больших чашек"});
        pluralForms.put("большое", new String[]{"больших", "больших"});
        pluralForms.put("большой стакан", new String[]{"больших стакана", "больших стаканов"});


        pluralForms.put("маленькая луковица", new String[]{"маленькие луковицы", "маленьких луковиц"});
        pluralForms.put("маленькая пачка", new String[]{"маленькие пачки", "маленьких пачек"});
        pluralForms.put("маленькая порция", new String[]{"маленькие порции", "маленьких порций"});
        pluralForms.put("маленькая чашка", new String[]{"маленькие чашки", "маленьких чашек"});
        pluralForms.put("маленький стакан", new String[]{"маленьких стакана", "маленьких стаканов"});
        pluralForms.put("маленькое", new String[]{"маленьких", "маленьких"});


        pluralForms.put("среднее", new String[]{"средних", "средних"});
        pluralForms.put("среднее (с1)", new String[]{"средних (С1)", "средних (С1)"});
        pluralForms.put("средний", new String[]{"средних", "средних"});
        pluralForms.put("средний гриб", new String[]{"средних гриба", "средних грибов"});
        pluralForms.put("средний лимон", new String[]{"средних лимона", "средних лимонов"});
        pluralForms.put("средняя", new String[]{"средних", "средних"});
        pluralForms.put("средняя грудка", new String[]{"средние грудки", "средних грудок"});
        pluralForms.put("средняя груша", new String[]{"средние груши", "средних груш"});
        pluralForms.put("средняя креветка", new String[]{"средние креветки", "средних креветок"});
        pluralForms.put("средняя луковица", new String[]{"средние луковицы", "средних луковиц"});


        pluralForms.put("горсть (4-5 шт)", new String[]{"горсти (4-5 шт)", "горстей (4-5 шт)"});
        pluralForms.put("горсть (5-6 шт)", new String[]{"горсти (5-6 шт)", "горстей (5-6 шт)"});
        pluralForms.put("горсть нарезанных", new String[]{"горсти нарезанных", "горстей нарезанных"});


        pluralForms.put("кусок (1/4)", new String[]{"куска (1/4)", "кусков (1/4)"});
        pluralForms.put("кусочек (1 см)", new String[]{"кусочка (1 см)", "кусочков (1 см)"});
        pluralForms.put("кусочек (на бутерброд)", new String[]{"кусочка (на бутерброд)", "кусочков (на бутерброд)"});


        pluralForms.put("порция (10-12 шт)", new String[]{"порции (10-12 шт)", "порций (10-12 шт)"});
        pluralForms.put("порция (15 шт.)", new String[]{"порции (15 шт.)", "порций (15 шт.)"});
        pluralForms.put("порция (4 шт)", new String[]{"порции (4 шт)", "порций (4 шт)"});
        pluralForms.put("порция (5-6 шт)", new String[]{"порции (5-6 шт)", "порций (5-6 шт)"});
        pluralForms.put("порция (6 шт)", new String[]{"порции (6 шт)", "порций (6 шт)"});
        pluralForms.put("порция для котлет", new String[]{"порции для котлет", "порций для котлет"});
        pluralForms.put("порция для салата", new String[]{"порции для салата", "порций для салата"});
        pluralForms.put("порция (из 2 яиц)", new String[]{"порции (из 2 яиц)", "порций (из 2 яиц)"});
        pluralForms.put("порция на бутерброд", new String[]{"порции на бутерброд", "порций на бутерброд"});
        pluralForms.put("порция на спагетти", new String[]{"порции на спагетти", "порций на спагетти"});
        pluralForms.put("порция на хлеб", new String[]{"порции на хлеб", "порций на хлеб"});
        pluralForms.put("порция (стейк)", new String[]{"порции (стейк)", "порций (стейк)"});
        pluralForms.put("порция (сухая)", new String[]{"порции (сухая)", "порций (сухая)"});
        pluralForms.put("порция (треугольник)", new String[]{"порции (треугольник)", "порций (треугольник)"});
        pluralForms.put("порция (филе)", new String[]{"порции (филе)", "порций (филе)"});
        pluralForms.put("порция (шинкованная)", new String[]{"порции (шинкованная)", "порций (шинкованная)"});


        pluralForms.put("от 1 яйца", new String[]{"от 2 яиц", "от яиц"});


        pluralForms.put("буррито", new String[]{"буррито", "буррито"});
    }


    public static String getPlural(float quantity, String word) {
        if (word == null || word.isEmpty()) return "";


        String lowerWord = word.toLowerCase();
        if (pluralForms.containsKey(lowerWord)) {
            String result = getPluralForm(quantity, lowerWord);

            if (Character.isUpperCase(word.charAt(0))) {
                result = result.substring(0, 1).toUpperCase() + result.substring(1);
            }
            return result;
        }


        String[] originalParts = word.split(" ");
        int nounIndex = -1;
        String nounToPluralize = null;


        for (int i = 0; i < originalParts.length; i++) {
            String part = originalParts[i].toLowerCase().replaceAll("[^а-яa-z-]", "");
            if (pluralForms.containsKey(part)) {
                nounToPluralize = part;
                nounIndex = i;
                break;
            }
        }


        if (nounToPluralize != null) {
            String pluralNoun = getPluralForm(quantity, nounToPluralize);


            if (Character.isUpperCase(originalParts[nounIndex].charAt(0))) {
                pluralNoun = pluralNoun.substring(0, 1).toUpperCase() + pluralNoun.substring(1);
            }

            originalParts[nounIndex] = pluralNoun;
            return String.join(" ", originalParts);
        }


        return word;
    }

    private static String getPluralForm(float quantity, String singleForm) {
        int n = (int) quantity;


        if (quantity != n) return pluralForms.get(singleForm)[0];
        n = Math.abs(n) % 100;
        int n1 = n % 10;

        if (n > 10 && n < 20) return pluralForms.get(singleForm)[1];
        if (n1 > 1 && n1 < 5) return pluralForms.get(singleForm)[0];
        if (n1 == 1) return singleForm;
        return pluralForms.get(singleForm)[1];
    }
}
