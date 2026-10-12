package plus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Coincidencia de etiquetas con clases de caracteres.
 * Soporta: literales, "|" (alternativas), [a-z], [^abc], escapes con "\" y los alfabetos:
 *   \d  digitos 0-9              \D  cualquier caracter que no es digito
 *   \w  letras y digitos A-Za-z0-9   \W  caracteres especiales (ni letra/digito ASCII, ni espacio)
 *   \s  espacio en blanco            \S  cualquier caracter que no es espacio en blanco
 * Los alfabetos pueden usarse solos (\d) o dentro de corchetes ([\dA-F], [^\w]).
 * Las clases negadas (\D, \W, \S) nunca coinciden con el blanco de la cinta (U+25A1).
 */
public final class LabelMatcher {
    public static final char TAPE_BLANK = '□';
    private static final Map<String, List<List<Atom>>> CACHE = Collections.synchronizedMap(new HashMap<String, List<List<Atom>>>());

    private LabelMatcher() {
    }

    /** true si el texto es una clase de caracteres: [..] o un alfabeto \d \D \w \W \s \S. */
    public static boolean isClassExpression(String s) {
        if (s == null) {
            return false;
        }
        if (s.length() == 2 && s.charAt(0) == '\\' && "dDwWsS".indexOf(s.charAt(1)) >= 0) {
            return true;
        }
        return s.length() >= 3 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']' && findClose(s, 0) == s.length() - 1;
    }

    /** true si el caracter c pertenece a la clase descrita por expr. */
    public static boolean matchesChar(String expr, char c) {
        return !consumable(expr, String.valueOf(c)).isEmpty();
    }

    public static SortedSet<Integer> consumable(String string, String string2) {
        TreeSet<Integer> treeSet = new TreeSet<Integer>();
        for (List<Atom> list : parse(string)) {
            if (list.size() > string2.length()) continue;
            boolean ok = true;
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).matches(string2.charAt(i))) continue;
                ok = false;
                break;
            }
            if (!ok) continue;
            treeSet.add(list.size());
        }
        return treeSet;
    }

    private static List<List<Atom>> parse(String string) {
        List<List<Atom>> cached = CACHE.get(string);
        if (cached != null) {
            return cached;
        }
        ArrayList<List<Atom>> alternatives = new ArrayList<List<Atom>>();
        ArrayList<Atom> current = new ArrayList<Atom>();
        int i = 0;
        int len = string.length();
        while (i < len) {
            char c = string.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char k = string.charAt(i + 1);
                current.add(isShorthand(k) ? shorthand(k) : literal(k));
                i += 2;
                continue;
            }
            if (c == '|') {
                alternatives.add(current);
                current = new ArrayList<Atom>();
                ++i;
                continue;
            }
            if (c == '[') {
                int close = findClose(string, i);
                if (close < 0) {
                    current.add(literal('['));
                    ++i;
                    continue;
                }
                current.add(charClass(string.substring(i + 1, close)));
                i = close + 1;
                continue;
            }
            current.add(literal(c));
            ++i;
        }
        alternatives.add(current);
        if (CACHE.size() > 5000) {
            CACHE.clear();
        }
        CACHE.put(string, alternatives);
        return alternatives;
    }

    private static int findClose(String string, int open) {
        for (int i = open + 1; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '\\') {
                ++i;
                continue;
            }
            if (c != ']' || i <= open + 1) continue;
            return i;
        }
        return -1;
    }

    private static boolean isShorthand(char k) {
        return "dDwWsS".indexOf(k) >= 0;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlnum(char c) {
        return isDigit(c) || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isSpace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    private static Atom shorthand(char k) {
        switch (k) {
            case 'd':
                return c -> isDigit(c);
            case 'D':
                return c -> !isDigit(c) && c != TAPE_BLANK;
            case 'w':
                return c -> isAlnum(c);
            case 'W':
                return c -> !isAlnum(c) && !isSpace(c) && c != TAPE_BLANK;
            case 's':
                return c -> isSpace(c);
            default:
                return c -> !isSpace(c) && c != TAPE_BLANK;
        }
    }

    private static Atom literal(final char c) {
        return c2 -> c2 == c;
    }

    private static Atom charClass(String string) {
        final boolean negate = string.startsWith("^") && string.length() > 1;
        if (negate) {
            string = string.substring(1);
        }
        final ArrayList<char[]> ranges = new ArrayList<char[]>();
        final ArrayList<Atom> shorthands = new ArrayList<Atom>();
        int i = 0;
        int len = string.length();
        while (i < len) {
            char c = string.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char k = string.charAt(i + 1);
                if (isShorthand(k)) {
                    shorthands.add(shorthand(k));
                    i += 2;
                    continue;
                }
                c = k;
                ++i;
            }
            if (i + 2 < len && string.charAt(i + 1) == '-') {
                char end = string.charAt(i + 2);
                if (end == '\\' && i + 3 < len) {
                    end = string.charAt(i + 3);
                    ++i;
                }
                ranges.add(new char[]{c, end});
                i += 3;
                continue;
            }
            ranges.add(new char[]{c, c});
            ++i;
        }
        return c -> {
            boolean in = false;
            for (char[] r : ranges) {
                if (c < r[0] || c > r[1]) continue;
                in = true;
                break;
            }
            if (!in) {
                for (Atom a : shorthands) {
                    if (!a.matches(c)) continue;
                    in = true;
                    break;
                }
            }
            return in != negate;
        };
    }

    interface Atom {
        boolean matches(char c);
    }
}
