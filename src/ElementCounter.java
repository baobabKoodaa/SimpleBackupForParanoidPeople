import java.util.HashMap;

/** Helper. */
public class ElementCounter {
    HashMap<String, Integer> elements;

    public ElementCounter() {
        elements = new HashMap<>();
    }

    public void add(String element) {
        int count = 1;
        Integer prev = elements.get(element);
        if (prev != null) count += prev;
        elements.put(element, count);
    }

    public void remove(String element) {
        int count = elements.remove(element);
        count--;
        if (count > 0) elements.put(element, count);
    }

    public int get(String element) {
        Integer val = elements.get(element);
        if (val == null) return 0;
        return val;
    }

    public int size() {
        return elements.size();
    }
}