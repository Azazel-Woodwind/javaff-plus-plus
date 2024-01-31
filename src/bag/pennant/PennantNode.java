package bag.pennant;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PennantNode<T> {
    List<T> elements;

    public PennantNode() {
        this.elements = new LinkedList<>();
    }

    public PennantNode(T value) {
        this();
        this.elements.addLast(value);
    }

    public PennantNode(List<T> elements) {
        this.elements = elements;
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public void add(T value) {
        elements.addLast(value);
    }

    public T removeLast() {
        return elements.removeLast();
    }

    public Iterator<T> iterator() {
        return elements.iterator();
    }
}
