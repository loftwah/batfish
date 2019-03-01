package org.batfish.common.bdd;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public final class BDDOps {
  private static final Field BDD_INDEX_FIELD = bddIndexField();
  private static final Method BDD_ADD_REF_METHOD = bddAddRefMethod();

  private final BDDFactory _factory;

  public BDDOps(BDDFactory factory) {
    _factory = factory;
  }

  public BDD and(BDD... conjuncts) {
    return and(Arrays.asList(conjuncts));
  }

  public BDD and(Iterable<BDD> conjuncts) {
    return firstNonNull(andNull(conjuncts), _factory.one());
  }

  /** A variant of {@link #and(BDD...)} that returns {@code null} when all conjuncts are null. */
  public static BDD andNull(BDD... conjuncts) {
    return andNull(Arrays.asList(conjuncts));
  }

  public static BDD andNull(Iterable<BDD> conjuncts) {
    BDD result = null;
    for (BDD conjunct : conjuncts) {
      if (conjunct != null) {
        result = result == null ? conjunct : result.and(conjunct);
      }
    }
    return result;
  }

  /** Returns bdd.not() or {@code null} if given {@link BDD} is null. */
  public static BDD negateIfNonNull(BDD bdd) {
    return bdd == null ? bdd : bdd.not();
  }

  private static Class<?> bddClass() {
    return Arrays.stream(JFactory.class.getDeclaredClasses())
        .filter(cls -> cls.getSimpleName().equals("bdd"))
        .findFirst()
        .get();
  }

  private static Field bddIndexField() {
    Class<?> bddClass = bddClass();
    try {
      Field field = bddClass.getDeclaredField("_index");
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static Method bddAddRefMethod() {
    try {
      Method method = JFactory.class.getDeclaredMethod("bdd_addref", int.class);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private static int getBddIndex(BDD bdd) throws IllegalAccessException {
    return (Integer) BDD_INDEX_FIELD.get(bdd);
  }

  private static void setBddIndex(BDD bdd, int index) throws IllegalAccessException {
    BDD_INDEX_FIELD.set(bdd, index);
  }

  private void addRef(int bddId) throws InvocationTargetException, IllegalAccessException {
    BDD_ADD_REF_METHOD.invoke(_factory, bddId);
  }

  public BDD orWithId(BDD... disjuncts) {
    BDD result = _factory.zero();
    for (BDD disjunct : disjuncts) {
      result.orWith(disjunct.id());
    }
    return result;
  }

  public BDD fastOr(BDD... disjuncts) {
    try {
      BDD result = _factory.zero();
      for (BDD disjunct : disjuncts) {
        int index = getBddIndex(disjunct);
        addRef(index);
        result.orWith(disjunct);
        setBddIndex(disjunct, index);
      }
      return result;
    } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public BDD or(BDD... disjuncts) {
    return or(Arrays.asList(disjuncts));
  }

  public BDD or(Iterable<BDD> disjuncts) {
    return firstNonNull(orNull(disjuncts), _factory.zero());
  }

  /** A variant of {@link #or(BDD...)} that returns {@code null} when all disjuncts are null. */
  public static BDD orNull(BDD... disjuncts) {
    return orNull(Arrays.asList(disjuncts));
  }

  public static BDD orNull(Iterable<BDD> disjuncts) {
    BDD result = null;
    for (BDD disjunct : disjuncts) {
      if (disjunct != null) {
        result = result == null ? disjunct : result.or(disjunct);
      }
    }
    return result;
  }
}
