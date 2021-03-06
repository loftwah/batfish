package org.batfish.specifier.parboiled;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.IpRange;
import org.batfish.datamodel.IpSpace;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationIpSpaceSpecifier;
import org.batfish.specifier.ReferenceAddressGroupIpSpaceSpecifier;
import org.batfish.specifier.SpecifierContext;

/**
 * An {@link IpSpaceSpecifier} that resolves based on the AST generated by {@link
 * org.batfish.specifier.parboiled.Parser}.
 */
final class ParboiledIpSpaceSpecifier implements IpSpaceSpecifier {

  static final class IpSpaceAstNodeToIpSpace implements IpSpaceAstNodeVisitor<IpSpace> {
    private final SpecifierContext _ctxt;

    IpSpaceAstNodeToIpSpace(SpecifierContext ctxt) {
      _ctxt = ctxt;
    }

    @Override
    @Nonnull
    public IpSpace visitUnionIpSpaceAstNode(UnionIpSpaceAstNode unionIpSpaceAstNode) {
      // using firstNonNull to stop compiler warning. since neither left nor right should will be
      // null, the union will not be null
      return firstNonNull(
          AclIpSpace.union(
              unionIpSpaceAstNode.getLeft().accept(this),
              unionIpSpaceAstNode.getRight().accept(this)),
          EmptyIpSpace.INSTANCE);
    }

    @Override
    @Nonnull
    public IpSpace visitIpAstNode(IpAstNode ipAstNode) {
      return ipAstNode.getIp().toIpSpace();
    }

    @Override
    @Nonnull
    public IpSpace visitIpRangeAstNode(IpRangeAstNode rangeIpSpaceAstNode) {
      return IpRange.range(rangeIpSpaceAstNode.getLow(), rangeIpSpaceAstNode.getHigh());
    }

    @Override
    @Nonnull
    public IpSpace visitIpWildcardAstNode(IpWildcardAstNode ipWildcardAstNode) {
      return ipWildcardAstNode.getIpWildcard().toIpSpace();
    }

    @Override
    @Nonnull
    public IpSpace visitLocationIpSpaceAstNode(LocationIpSpaceAstNode locationIpSpaceAstNode) {
      return LocationIpSpaceSpecifier.computeIpSpace(
          new ParboiledLocationSpecifier(locationIpSpaceAstNode.getLocationAst()).resolve(_ctxt),
          _ctxt);
    }

    @Override
    @Nonnull
    public IpSpace visitPrefixAstNode(PrefixAstNode prefixAstNode) {
      return prefixAstNode.getPrefix().toIpSpace();
    }

    @Override
    @Nonnull
    public IpSpace visitAddressGroupAstNode(AddressGroupIpSpaceAstNode addressGroupIpSpaceAstNode) {
      return ReferenceAddressGroupIpSpaceSpecifier.computeIpSpace(
          addressGroupIpSpaceAstNode.getAddressGroup(),
          addressGroupIpSpaceAstNode.getAddressBook(),
          _ctxt);
    }
  }

  private final IpSpaceAstNode _ast;

  ParboiledIpSpaceSpecifier(IpSpaceAstNode ast) {
    _ast = ast;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParboiledIpSpaceSpecifier)) {
      return false;
    }
    return Objects.equals(_ast, ((ParboiledIpSpaceSpecifier) o)._ast);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ast);
  }

  @Override
  public IpSpaceAssignment resolve(Set<Location> locations, SpecifierContext ctxt) {
    IpSpace ipSpace = computeIpSpace(_ast, ctxt);
    return IpSpaceAssignment.builder().assign(locations, ipSpace).build();
  }

  @VisibleForTesting
  @Nonnull
  static IpSpace computeIpSpace(IpSpaceAstNode ast, SpecifierContext ctxt) {
    return ast.accept(new IpSpaceAstNodeToIpSpace(ctxt));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("ast", _ast).toString();
  }
}
