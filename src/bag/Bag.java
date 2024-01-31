package bag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bag.pennant.Pennant;
import bag.pennant.PennantNode;
import utils.Pair;

interface IBag<T> {
    void insert(T value);

    IBag<T> split();

    void union(IBag<T> other);

    int size();

    boolean isEmpty();
}

public class Bag<T> implements IBag<T>, Iterable<T> {
    private List<Pennant<T>> spine;
    private PennantNode<T> hopper;
    public static final int GRAIN_SIZE = 16;
    // public static final int GRAIN_SIZE = 99999999;

    public Bag() {
        this.spine = new ArrayList<>();
        hopper = new PennantNode<>();
    }

    // public Bag(List<Pennant<T>> spine) {
    // this.spine = spine;
    // hopper = new Pennant<>();
    // }
    @Override
    public void insert(T value) {
        if (hopper.size() == GRAIN_SIZE) {
            this.insert(hopper);
            hopper = new PennantNode<>(value);
        } else {
            hopper.add(value);
        }
    }

    private void insert(PennantNode<T> node) {
        Pennant<T> newPennant = new Pennant<>(node);
        insert(newPennant);
    }

    private void insert(Pennant<T> newPennant) {
        int k = 0;
        while (k < spine.size() && spine.get(k) != null) {
            Pennant<T> pennant = spine.get(k);
            spine.set(k, null);
            newPennant.union(pennant);
            k++;
        }

        if (k < spine.size()) {
            spine.set(k, newPennant);
        } else {
            spine.add(newPennant);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new BagIterator();
    }

    private class BagIterator implements Iterator<T> {
        private int currentIndex;
        Iterator<T> currentIterator;

        BagIterator() {
            if (hopper.size() > 0) {
                currentIterator = hopper.iterator();
                currentIndex = -1;
            } else {
                currentIndex = 0;
                while (currentIndex < spine.size() && spine.get(currentIndex) == null) {
                    currentIndex++;
                }
                if (currentIndex < spine.size()) {
                    currentIterator = spine.get(currentIndex).iterator();
                }
            }
        }

        @Override
        public boolean hasNext() {
            return currentIterator != null && currentIterator.hasNext();
        }

        @Override
        public T next() {
            T value = currentIterator.next();
            if (!currentIterator.hasNext()) {
                currentIndex++;
                while (currentIndex < spine.size() && spine.get(currentIndex) == null) {
                    currentIndex++;
                }
                if (currentIndex < spine.size()) {
                    currentIterator = spine.get(currentIndex).iterator();
                }
            }
            return value;
        }
    }

    @Override
    public IBag<T> split() {
        Bag<T> newBag = new Bag<>();
        if (spine.size() > 0) {
            if (spine.get(0) != null) {
                newBag.hopper = spine.get(0).getRoot();
            }
            spine.set(0, null);
        }

        for (int k = 1; k < spine.size(); k++) {
            Pennant<T> firstHalf = spine.get(k);
            if (firstHalf == null) {
                newBag.spine.add(null);
                continue;
            }

            Pennant<T> secondHalf = (Pennant<T>) firstHalf.split();
            newBag.spine.add(firstHalf);
            spine.set(k, null);
            spine.set(k - 1, secondHalf);
        }

        // if (y != null) {
        // insert(y);
        // }

        return newBag;
    }

    @Override
    public void union(IBag<T> other) {
        Bag<T> otherBag = (Bag<T>) other;

        Bag<T> smallerHopperBag, largerHopperBag;
        if (hopper.size() <= otherBag.hopper.size()) {
            smallerHopperBag = this;
            largerHopperBag = otherBag;
        } else {
            smallerHopperBag = otherBag;
            largerHopperBag = this;
        }

        while (smallerHopperBag.hopper.size() > 0
                && largerHopperBag.hopper.size() < GRAIN_SIZE) {

            largerHopperBag.hopper.add(
                    smallerHopperBag.hopper.removeLast());
        }

        Pennant<T> carry = null;
        if (largerHopperBag.hopper.size() == GRAIN_SIZE) {
            carry = new Pennant<>(largerHopperBag.hopper);
            hopper = smallerHopperBag.hopper;
        } else {
            hopper = largerHopperBag.hopper;
        }

        int k = 0;
        // System.out.println("Spine size: " + spine.size() + " Other spine size: " +
        // otherBag.spine.size());
        while (carry != null || k < otherBag.spine.size())
        // while (k < spine.size() || k < otherBag.spine.size())
        {
            // System.out.println("K: " + k);
            Pennant<T> x = k < spine.size() ? spine.get(k) : null;
            Pennant<T> y = k < otherBag.spine.size() ? otherBag.spine.get(k) : null;
            Pair<Pennant<T>, Pennant<T>> result = fullAdder(x, y, carry);
            Pennant<T> sum = result.getFirst();
            carry = result.getSecond();
            if (k < spine.size()) {
                spine.set(k, sum);
            } else {
                spine.add(sum);
            }
            k++;
        }

        // if (carry != null) {
        // spine.add(carry);
        // }
    }

    private Pair<Pennant<T>, Pennant<T>> fullAdder(Pennant<T> x, Pennant<T> y, Pennant<T> z) {
        Pennant<T> sum = null;
        Pennant<T> carry = null;

        int count = 0;
        if (x != null)
            count++;
        if (y != null)
            count++;
        if (z != null)
            count++;

        switch (count) {
            case 1:
                sum = x != null ? x : (y != null ? y : z);
                break;
            case 2:
                if (x != null && y != null) {
                    x.union(y);
                    carry = x;
                } else if (x != null && z != null) {
                    x.union(z);
                    carry = x;
                } else {
                    y.union(z);
                    carry = y;
                }
                break;
            case 3:
                sum = x;
                y.union(z);
                carry = y;
                break;
        }

        return new Pair<>(sum, carry);
    }

    @Override
    public int size() {
        int size = hopper.size();
        for (Pennant<T> pennant : spine) {
            if (pennant != null) {
                size += Math.pow(2, spine.indexOf(pennant)) * GRAIN_SIZE;
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return hopper.isEmpty() && spine.isEmpty();
    }
}