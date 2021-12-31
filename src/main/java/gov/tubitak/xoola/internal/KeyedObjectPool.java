package gov.tubitak.xoola.internal;

import gov.tubitak.xoola.exception.XoolaException;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * An object pool that allows to borrow keys and then get objects assoicated with that key.
 * <p>
 * Used for message flagging in multithreaded async. communication.
 *
 * @param <K> The Key
 * @param <V> The Value
 */
public class KeyedObjectPool<K, V> {

  public static <K, V> KeyedObjectPool<K, V> createFixSizedPool(int poolSize) {
    return new KeyedObjectPool<>(poolSize);
  }

  private HashMap<K, V> map;
  private int poolSize;
  private PriorityBlockingQueue<K> keyQueue;

  private KeyedObjectPool(int poolSize) {
    map = new HashMap<K, V>(poolSize);
    this.poolSize = poolSize;
    keyQueue = new PriorityBlockingQueue<K>();
  }

  public K borrowKey() {
    if (keyQueue.size() < poolSize){
      
    }
    try {
      return keyQueue.take();
    } catch (InterruptedException e) {
      throw new XoolaException(e);
    }
  }

  public V getObject(K key) {
    return map.get(key);
  }

  public void putBack(K key, V value) {
    keyQueue.add(key);
  }
}
