package bag.pennant;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

interface IPennant<T> {
    IPennant<T> split();

    void union(IPennant<T> other);
}

public class Pennant<T> implements IPennant<T>, Iterable<T> {
    public static class Root<T> extends PennantNode<T> {
        public TreeNode<T> child;

        public Root() {
            this.child = null;
        }

        public Root(T value) {
            super(value);
            this.child = null;
        }

        public Root(List<T> elements) {
            super(elements);
            this.child = null;
        }

        public Root(TreeNode<T> child) {
            this.child = child;
        }
    }

    public static class TreeNode<T> extends PennantNode<T> {
        public TreeNode<T> left;
        public TreeNode<T> right;

        public TreeNode() {
            this.left = null;
            this.right = null;
        }

        public TreeNode(TreeNode<T> left, TreeNode<T> right) {
            this.left = left;
            this.right = right;
        }

        public TreeNode(List<T> elements, TreeNode<T> left, TreeNode<T> right) {
            super(elements);
            this.left = left;
            this.right = right;
        }

        public int size() {
            int size = 1;
            if (left != null) {
                size += left.size();
            }
            if (right != null) {
                size += right.size();
            }
            return size;
        }
    }

    private Root<T> root;

    public Pennant() {
        this.root = new Root<>();
    }

    public Pennant(T rootValue) {
        this.root = new Root<>(rootValue);
    }

    public Pennant(List<T> elements) {
        this.root = new Root<>();
        this.root.elements = elements;
    }

    public Pennant(PennantNode<T> node) {
        this.root = new Root<>(node.elements);
    }

    public Pennant(List<T> elements, TreeNode<T> child) {
        this.root = new Root<>(child);
        this.root.elements = elements;
    }

    public PennantNode<T> getRoot() {
        return root;
    }

    public Iterator<T> iterator() {
        return new PennantIterator();
    }

    private class PennantIterator implements Iterator<T> {
        private Queue<PennantNode<T>> queue;
        private Iterator<T> currentIterator;

        PennantIterator() {
            queue = new LinkedList<>();
            queue.add(root);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty() || currentIterator.hasNext();
        }

        @Override
        public T next() {
            if (currentIterator == null || !currentIterator.hasNext()) {
                PennantNode<T> node = queue.poll();
                if (node instanceof Root) {
                    Root<T> root = (Root<T>) node;
                    if (root.child != null) {
                        queue.add(root.child);
                    }
                } else {
                    TreeNode<T> TreeNode = (TreeNode<T>) node;
                    if (TreeNode.left != null) {
                        queue.add(TreeNode.left);
                    }
                    if (TreeNode.right != null) {
                        queue.add(TreeNode.right);
                    }
                }

                currentIterator = node.elements.iterator();
            }
            return currentIterator.next();
        }
    }

    private TreeNode<T> getChild() {
        return root.child;
    }

    @Override
    public void union(IPennant<T> other) {
        Pennant<T> otherPennant = (Pennant<T>) other;
        TreeNode<T> newTreeRoot = new TreeNode<>(otherPennant.root.elements, root.child, otherPennant.getChild());
        root.child = newTreeRoot;
    }

    @Override
    public IPennant<T> split() {
        Pennant<T> newPennant = new Pennant<>(root.child.elements, root.child.right);
        root.child = root.child.left;
        return newPennant;
    }

    public int size() {
        int size = 1;
        if (root.child != null) {
            size += root.child.size();
        }
        return size;
    }
}
