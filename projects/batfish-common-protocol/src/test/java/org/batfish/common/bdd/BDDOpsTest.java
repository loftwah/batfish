package org.batfish.common.bdd;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Random;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;
import org.junit.Before;
import org.junit.Test;

public class BDDOpsTest {
  private BDDFactory _factory;
  private BDDOps _bddOps;

  @Before
  public void init() {
    _factory = JFactory.init(10000, 1000);
    _factory.disableReorder();
    _factory.setCacheRatio(64);
    _bddOps = new BDDOps(_factory);
  }

  @Test
  public void testAnd_null() {
    assertThat(_bddOps.and(), equalTo(_factory.one()));
    assertThat(_bddOps.and(null, null), equalTo(_factory.one()));
  }

  @Test
  public void testAnd_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.and(var, _factory.one()), equalTo(var));
  }

  @Test
  public void testAnd_var_varNot() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.and(var, var.not()), equalTo(_factory.zero()));
  }

  @Test
  public void testAnd_zero() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.and(var, _factory.zero()), equalTo(_factory.zero()));
  }

  @Test
  public void testAndNull_null() {
    assertThat(BDDOps.andNull(), nullValue());
    assertThat(BDDOps.andNull(null, null), nullValue());
  }

  @Test
  public void testAndNull_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(BDDOps.andNull(var, null), equalTo(var));
  }

  @Test
  public void testAndNull_two() {
    _factory.setVarNum(2);
    BDD var1 = _factory.ithVar(0);
    BDD var2 = _factory.ithVar(1);
    assertThat(BDDOps.andNull(var1, var2, null), equalTo(var1.and(var2)));
  }

  @Test
  public void testOr_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.or(var, _factory.one()), equalTo(_factory.one()));
  }

  @Test
  public void testOr_null() {
    assertThat(_bddOps.or(), equalTo(_factory.zero()));
    assertThat(_bddOps.or(null, null), equalTo(_factory.zero()));
  }

  @Test
  public void testOr_var_varNot() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.or(var, var.not()), equalTo(_factory.one()));
  }

  @Test
  public void testOr_zero() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.or(var, _factory.zero()), equalTo(var));
  }

  @Test
  public void testOrNull_null() {
    assertThat(BDDOps.orNull(), nullValue());
    assertThat(BDDOps.orNull(null, null), nullValue());
  }

  @Test
  public void testOrNull_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(BDDOps.orNull(var, null), equalTo(var));
  }

  @Test
  public void testOrNull_two() {
    _factory.setVarNum(2);
    BDD var1 = _factory.ithVar(0);
    BDD var2 = _factory.ithVar(1);
    assertThat(BDDOps.orNull(null, var1, var2), equalTo(var1.or(var2)));
  }

  @Test
  public void benchmarkOr() {
    Random random = new Random();

    int numDisjuncts = 2000;
    int numRounds = 60; // how many different sets of disjuncts
    int numIters = 50; // how many times we or each set of disjuncts

    long orTime = 0;
    long fastOrTime = 0;
    long orWithIdTime = 0;

    for (int round = 0; round < numRounds; round++) {
      // reset timers after warmup
      if (round == 10) {
        orTime = 0;
        fastOrTime = 0;
        orWithIdTime = 0;
      }

      // reinitialize bdd factory
      init();
      _factory.setVarNum(32);
      BDDInteger bddInteger = BDDInteger.makeFromIndex(_factory, 32, 0, false);

      System.gc();

      BDD[] disjuncts = new BDD[numDisjuncts];
      for (int i = 0; i < numDisjuncts; i++) {
        disjuncts[i] = bddInteger.value(random.nextInt(2 ^ 32));
      }

      BDD or1 = null;
      {
        long t = System.currentTimeMillis();
        for (int i = 0; i < numIters; i++) {
          or1 = _bddOps.or(disjuncts);
        }
        orTime += System.currentTimeMillis() - t;
      }

      BDD or2 = null;
      {
        long t = System.currentTimeMillis();
        for (int i = 0; i < numIters; i++) {
          or2 = _bddOps.fastOr(disjuncts);
        }
        fastOrTime += System.currentTimeMillis() - t;
      }

      BDD or3 = null;
      {
        long t = System.currentTimeMillis();
        for (int i = 0; i < numIters; i++) {
          or3 = _bddOps.orWithId(disjuncts);
        }
        orWithIdTime += System.currentTimeMillis() - t;
      }

      assertEquals(or1, or2);
      assertEquals(or1, or3);
    }

    System.out.println(String.format("or=%d", orTime));
    System.out.println(String.format("orWithId=%d", orWithIdTime));
    System.out.println(String.format("fastOr=%d", fastOrTime));
  }
}
