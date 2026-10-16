package test.modern;

import java.util.Iterator;
import java.util.Map;

/**
 * Source in the style printed by JD-Core: explicit 'this', casts, labels, synthetic-looking names.
 */
public class Decompiled<K, V> {
  private final Map<K, V> map;
  
  private int modCount;
  
  public Decompiled(Map<K, V> map) {
    this.map = map;
  }
  
  public V find(K key) {
    for (Iterator<Map.Entry<K, V>> iterator = this.map.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry<K, V> entry = iterator.next();
      if (entry.getKey().equals(key))
        return entry.getValue(); 
    } 
    return null;
  }
  
  public int count(Object[] values) {
    int i = 0;
    label: for (int j = 0; j < values.length; j++) {
      Object object = values[j];
      if (object == null)
        continue label; 
      if (object instanceof String) {
        String str = (String)object;
        if (str.isEmpty())
          break; 
      } 
      i++;
    } 
    this.modCount++;
    return i;
  }
  
  static class Inner extends Decompiled<String, Integer> {
    Inner(Map<String, Integer> paramMap) {
      super(paramMap);
    }
  }
}
