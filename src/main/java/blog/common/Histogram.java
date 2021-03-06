/*
 * Copyright (c) 2005, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 * 
 * * Neither the name of the University of California, Berkeley nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior
 * written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package blog.common;

import java.io.PrintStream;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A mapping from objects to log-weights, which are real numbers. A Histogram
 * maintains an internal map from objects to Doubles, and also keeps track of
 * the log-sum of the weights. If the <code>sorted</code> flag is specified at
 * construction, then the Histogram uses a sorted map so that sampling results
 * are reproducible.
 * 
 * <p>
 * A histogram conceptually stores a weight for every object, but most objects
 * have zero weight. Only objects with non-zero weight are explicitly
 * represented. The <code>iterator</code> method only allows you to iterate over
 * objects with non-zero weight; if you want to iterate over other objects, you
 * need to enumerate those objects in some other way, and call
 * <code>getLogWeight</code> for each one.
 *
 * The class accepts a normalizer, that is, a UnaryFunction mapping each
 * possible element to a representative in a user-defined equivalency class. The
 * representative is the one stored in the histogram. By default, the normalizer
 * is the identity function.
 * 
 * <p>
 * Note: This class does not allow null elements to be added.
 */
public class Histogram implements SetWithDistrib {
  /**
   * Creates an empty histogram.
   */
  public Histogram() {
    initMap();
  }

  /**
   * Creates an empty histogram.
   * 
   * @param sorted
   *          if true, use a sorted map from elements to weights
   */
  public Histogram(boolean sorted) {
    this.sorted = sorted;
    initMap();
  }

  public UnaryFunction getNormalizer() {
    return normalizer;
  }

  public void setNormalizer(UnaryFunction normalizer) {
    this.normalizer = normalizer;
  }

  /**
   * Returns the number of objects that have non-zero weight in this histogram.
   */
  public int size() {
    return map.size();
  }

  /**
   * Returns the log weight of the given object in this histogram. If the
   * object is not explicitly represented in the histogram, its log weight is
   * -inf.
   */
  public double getLogWeight(Object obj) {
    if (obj == null) {
        return Double.NEGATIVE_INFINITY;
    }

    Double value = (Double) map.get(normalizer.evaluate(obj));
    if (value == null) {
        return Double.NEGATIVE_INFINITY;
    }
    return value.doubleValue();
  }

  /**
   * Return the log of the sum of the weights of all objects in this histogram.
   */
  public double getTotalLogWeight() {
    return totalLogWeight;
  }

  /**
   * Return the probability of the given object being sampled, according to its
   * weight.
   */
  public double getProb(Object o) {
    return java.lang.Math.exp(getLogProb(o));
  }

  /**
   * Returns the log probability of the given object being sampled, according to
   * its weight.
   */
  public double getLogProb(Object o) {
    Double value = (Double) map.get(normalizer.evaluate(o));
    if(value == null) {
        return Double.NEGATIVE_INFINITY;
    }
    return value - totalLogWeight;
  }

  /**
   * Increases the weight for the given object by the given amount. If the
   * object was not explicitly represented in the histogram, its old weight is
   * considered to be zero.
   */
  public void increaseWeight(Object obj, double logWeight) {
      totalLogWeight = Util.logSum(totalLogWeight, logWeight);
      map.put(obj, Util.logSum(getLogWeight(obj), logWeight));
  }

  /**
   * Resets the weights of all objects to zero.
   */
  public void clear() {
    initMap();
    totalLogWeight = Double.NEGATIVE_INFINITY;
  }

  /**
   * Returns an unmodifiable view of the set of objects that have non-zero
   * weight in this histogram.
   */
  public Set elementSet() {
    return Collections.unmodifiableMap(map).keySet();
  }

  /**
   * Returns an unmodifiable view of the set of Histogram.Entry objects
   * corresponding to the non-zero weight objects in this histogram.
   */
  public Set entrySet() {
    return entrySet;
  }

  public Object sample() {
    // FIXME: make this use log weights
    return null;

    /*
    double remaining = Util.random() * totalWeight;
    Object o = null;
    for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      o = entry.getKey();
      remaining -= ((Double) entry.getValue()).doubleValue();
      if (remaining < 0) {
        break;
      }
    }
    return o;
    */
  }

  /**
   * Prints this histogram to the given stream. Each entry is printed out on its
   * own line: first the key, then the value. If this histogram was constructed
   * with the <code>sorted</code> flag set to true, then the entries are sorted
   * by key.
   */
  public void print(PrintStream s) {
    for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      s.print(entry.getKey());
      s.print('\t');
      s.println(entry.getValue());
    }
  }

  private static Comparator WEIGHT_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      double diff = ((Double) ((Map.Entry) o1).getValue()).doubleValue()
          - ((Double) ((Map.Entry) o2).getValue()).doubleValue();
      if (diff < 0) {
        return 1;
      } else if (diff > 0) {
        return -1;
      }
      return 0;
    }
  };

  /**
   * Remove entries from histogram such that at least
   * <code>(1 - percentile)</code> of the total weight remains.
   */
  public void prune(double percentile) {
    // FIXME: make this use log weights

    /*
    List entries = new ArrayList(map.entrySet());
    Collections.sort(entries, WEIGHT_COMPARATOR);
    double remainingWeight = totalWeight;
    double toBePruned = percentile * totalWeight;

    // skip entries to be kept
    Iterator it = entries.iterator();
    while (it.hasNext() && remainingWeight >= toBePruned) {
      Map.Entry entry = (Map.Entry) it.next();
      double weight = ((Double) entry.getValue()).doubleValue();
      remainingWeight -= weight;
    }

    // set remaining entries weights to zero.
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      increaseWeight(entry.getKey(), -((Double) entry.getValue()).doubleValue());
    }
    */
  }

  /**
   * Returns a collection with up to <code>n</code> entries, and with the
   * minimum number of elements comprising at least <code>percentile</code> of
   * total mass.
   */
  public Collection getNBestButInUpper(int n, double percentile) {
    // FIXME: make this use log weights
    return null;

    /*
    List entries = new ArrayList(map.entrySet());
    Collections.sort(entries, WEIGHT_COMPARATOR);
    double remainingWeight = totalWeight;
    double toBePruned = (1 - percentile) * totalWeight;
    Collection result = new LinkedList();

    Iterator it = entries.iterator();
    while (it.hasNext() && remainingWeight > toBePruned && result.size() < n) {
      Map.Entry entry = (Map.Entry) it.next();
      double weight = ((Double) entry.getValue()).doubleValue();
      remainingWeight -= weight;
      result.add(entry.getKey());
    }

    return result;
    */
  }

  private void initMap() {
    if (sorted) {
      map = new TreeMap();
    } else {
      map = new HashMap();
    }
  }

  /**
   * Nested class for the entries in a histogram.
   */
  public static class Entry {
    Entry(Object obj, double logWeight) {
      this.obj = obj;
      this.logWeight = logWeight;
    }

    /**
     * Returns the object in this entry.
     */
    public Object getElement() {
      return obj;
    }

    /**
     * Returns the log weight of the object.
     */
    public double getLogWeight() {
      return logWeight;
    }

    public boolean equals(Object o) {
      if (o instanceof Histogram.Entry) {
        Histogram.Entry other = (Histogram.Entry) o;
        return (other.getElement().equals(obj) && (other.getLogWeight() == logWeight));
      }
      return false;
    }

    public int hashCode() {
      return obj.hashCode();
    }

    public String toString() {
      return ("(" + obj + ", " + logWeight + ")");
    }

    private Object obj;
    private double logWeight;
  }

  private class EntrySet extends AbstractSet {
    public int size() {
      return map.size();
    }

    public Iterator iterator() {
      return new EntrySetIterator();
    }

    private class EntrySetIterator implements Iterator {
      public boolean hasNext() {
        return mapIter.hasNext();
      }

      public Object next() {
        Map.Entry mapEntry = (Map.Entry) mapIter.next();
        double logWeight = ((Double) mapEntry.getValue()).doubleValue();
        return new Histogram.Entry(mapEntry.getKey(), logWeight);
      }

      public void remove() {
        throw new UnsupportedOperationException(
            "Histogram entry set iterator does not allow removal.");
      }

      private Iterator mapIter = map.entrySet().iterator();
    }
  }

  private boolean sorted = false;
  private Map map;  // map of object to logWeight
  private double totalLogWeight = Double.NEGATIVE_INFINITY;
  private EntrySet entrySet = new Histogram.EntrySet();
  private UnaryFunction normalizer = IdentityFunction.getInstance();
}
