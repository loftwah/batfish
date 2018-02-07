package org.batfish.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;

class KDTree {

  private int _dimensions;
  private KNode _root;

  KDTree() {
    _dimensions = 1;
    _root = null;
  }

  private int nextDim(int cd) {
    return (cd + 1) % (_dimensions + 1);
  }

  private long getDim(HyperRectangle r, int dim) {
    return dim == 0 ? r.getX1() : r.getX2();
  }

  // A utility function to find minimum of three integers
  @Nullable
  private HyperRectangle min(@Nullable HyperRectangle x, @Nullable HyperRectangle y, int dim) {
    if (x == null) {
      return y;
    }
    if (y == null) {
      return x;
    }
    if (getDim(x, dim) > getDim(y, dim)) {
      return y;
    }
    return x;
  }

  @Nullable
  private HyperRectangle min(int dim, @Nullable HyperRectangle x, @Nullable HyperRectangle y) {
    if (x == null) {
      return y;
    }
    if (y == null) {
      return x;
    }
    long xx = dim == 0 ? x.getX1() : x.getX2();
    long yy = dim == 0 ? y.getX1() : y.getX2();
    if (xx < yy) {
      return x;
    } else {
      return y;
    }
  }

  private KNode insert(HyperRectangle r, @Nullable KNode t, int cd) {
    if (t == null) {
      return new KNode(r, null, null);
    }
    long x1 = cd == 0 ? r.getX1() : r.getX2();
    long x2 = cd == 0 ? t._rectangle.getX1() : t._rectangle.getX2();
    if (r.equals(t._rectangle)) {
      System.out.println("ADDING DUPLICATE: " + r);
    } else if (x1 < x2) {
      t._left = insert(r, t._left, nextDim(cd));
    } else {
      t._right = insert(r, t._right, nextDim(cd));
    }
    return t;
  }

  @Nullable
  private HyperRectangle findMin(KNode t, int dim, int cd) {
    if (t == null) {
      return null;
    }
    if (cd == dim) {
      if (t._left == null) {
        return t._rectangle;
      } else {
        return findMin(t._left, dim, nextDim(cd));
      }
    } else {
      HyperRectangle x = findMin(t._left, dim, nextDim(cd));
      HyperRectangle y = findMin(t._right, dim, nextDim(cd));
      return min(min(x, y, dim), t._rectangle, dim);
    }
  }

  @Nullable
  private KNode delete(@Nullable HyperRectangle r, KNode t, int cd) {
    if (t == null) {
      throw new BatfishException("Delete KD tree not found: " + r);
    }
    int nextCd = nextDim(cd);
    if (Objects.equals(r, t._rectangle)) {
      if (t._right != null) {
        t._rectangle = findMin(t._right, cd, nextCd);
        t._right = delete(t._rectangle, t._right, nextCd);
      } else if (t._left != null) {
        t._rectangle = findMin(t._left, cd, nextCd);
        t._right = delete(t._rectangle, t._left, nextCd);
        t._left = null;
      } else {
        t = null;
      }
      return t;
    }
    if (getDim(r, cd) < getDim(t._rectangle, cd)) {
      t._left = delete(r, t._left, nextCd);
    } else {
      t._right = delete(r, t._right, nextCd);
    }
    return t;
  }

  private void intersect(HyperRectangle r, KNode t, int cd, List<HyperRectangle> result) {
    if (t == null) {
      return;
    }
    int nextCd = nextDim(cd);

    if (r.overlap(t._rectangle) != null) {
      result.add(t._rectangle);
    }

    if (cd == 0) {
      // branching on low, so if search rect has high lower than low, we skip
      if (r.getX2() < t._rectangle.getX1()) {
        intersect(r, t._left, nextCd, result);
      } else {
        intersect(r, t._left, nextCd, result);
        intersect(r, t._right, nextCd, result);
      }
    }

    if (cd == 1) {
      // branching on high, so if seach rect has low higher than high, we skip
      if (r.getX1() > t._rectangle.getX2()) {
        intersect(r, t._right, nextCd, result);
      } else {
        intersect(r, t._left, nextCd, result);
        intersect(r, t._right, nextCd, result);
      }
    }
  }

  private int size(KNode t) {
    if (t == null) {
      return 0;
    }
    return 1 + size(t._left) + size(t._right);
  }

  private void elements(KNode t, List<HyperRectangle> elems) {
    if (t == null) {
      return;
    }
    elems.add(t._rectangle);
    elements(t._left, elems);
    elements(t._right, elems);
  }

  int size() {
    return size(_root);
  }

  List<HyperRectangle> elements() {
    List<HyperRectangle> elems = new ArrayList<>();
    elements(_root, elems);
    return elems;
  }

  List<HyperRectangle> intersect(HyperRectangle r) {
    List<HyperRectangle> allIntersects = new ArrayList<>();
    intersect(r, _root, 0, allIntersects);
    return allIntersects;
  }

  void insert(HyperRectangle r) {
    _root = insert(r, _root, 0);
  }

  void delete(HyperRectangle r) {
    _root = delete(r, _root, 0);
  }

  class KNode {
    private HyperRectangle _rectangle;
    private KNode _left;
    private KNode _right;

    KNode(HyperRectangle rect, @Nullable KNode l, @Nullable KNode r) {
      this._rectangle = rect;
      this._left = l;
      this._right = r;
    }
  }
}
