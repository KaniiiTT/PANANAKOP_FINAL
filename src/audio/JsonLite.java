package audio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny JSON reader (objects, arrays, strings, numbers, booleans, null) so no external library is needed. */
final class JsonLite {
	private final String s;
	private int i;

	private JsonLite(String text) {
		this.s = text;
	}

	static Object parse(String text) {
		JsonLite p = new JsonLite(text);
		p.ws();
		Object v = p.value();
		p.ws();
		if (p.i != p.s.length())
			throw new IllegalArgumentException("Trailing data at " + p.i);
		return v;
	}

	private Object value() {
		char c = peek();
		switch (c) {
		case '{': return object();
		case '[': return array();
		case '"': return string();
		case 't': expect("true"); return Boolean.TRUE;
		case 'f': expect("false"); return Boolean.FALSE;
		case 'n': expect("null"); return null;
		default: return number();
		}
	}

	private Map<String, Object> object() {
		Map<String, Object> m = new LinkedHashMap<>();
		i++;
		ws();
		if (peek() == '}') { i++; return m; }
		while (true) {
			ws();
			String k = string();
			ws();
			if (s.charAt(i++) != ':')
				throw new IllegalArgumentException("Expected ':' at " + i);
			ws();
			m.put(k, value());
			ws();
			char c = s.charAt(i++);
			if (c == '}') return m;
			if (c != ',')
				throw new IllegalArgumentException("Expected ',' at " + i);
		}
	}

	private List<Object> array() {
		List<Object> l = new ArrayList<>();
		i++;
		ws();
		if (peek() == ']') { i++; return l; }
		while (true) {
			ws();
			l.add(value());
			ws();
			char c = s.charAt(i++);
			if (c == ']') return l;
			if (c != ',')
				throw new IllegalArgumentException("Expected ',' at " + i);
		}
	}

	private String string() {
		if (s.charAt(i) != '"')
			throw new IllegalArgumentException("Expected string at " + i);
		i++;
		StringBuilder b = new StringBuilder();
		while (true) {
			char c = s.charAt(i++);
			if (c == '"') return b.toString();
			if (c == '\\') {
				char e = s.charAt(i++);
				switch (e) {
				case 'n': b.append('\n'); break;
				case 't': b.append('\t'); break;
				case 'r': b.append('\r'); break;
				case 'b': b.append('\b'); break;
				case 'f': b.append('\f'); break;
				case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
				default: b.append(e);
				}
			} else
				b.append(c);
		}
	}

	private Double number() {
		int st = i;
		while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0)
			i++;
		if (st == i)
			throw new IllegalArgumentException("Unexpected character at " + i);
		return Double.valueOf(s.substring(st, i));
	}

	private void expect(String word) {
		if (!s.startsWith(word, i))
			throw new IllegalArgumentException("Expected " + word + " at " + i);
		i += word.length();
	}

	private char peek() {
		return s.charAt(i);
	}

	private void ws() {
		while (i < s.length() && Character.isWhitespace(s.charAt(i)))
			i++;
	}
}
