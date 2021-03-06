package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.ospf.OspfMetricType;

/** Base class for OSPF external routes */
@ParametersAreNonnullByDefault
public abstract class OspfExternalRoute extends OspfRoute {

  public static final class Builder extends AbstractRouteBuilder<Builder, OspfExternalRoute> {

    @Nullable private String _advertiser;
    @Nullable private Long _area;
    @Nullable private Long _costToAdvertiser;
    @Nullable private Long _lsaMetric;
    @Nullable private OspfMetricType _ospfMetricType;

    @Nonnull
    @Override
    public OspfExternalRoute build() {
      checkArgument(_ospfMetricType != null, "Missing OSPF external metric type");
      checkArgument(_lsaMetric != null, "Missing %s", PROP_LSA_METRIC);
      checkArgument(_area != null, "Missing %s", PROP_AREA);
      checkArgument(_costToAdvertiser != null, "Missing OSPF %s", PROP_COST_TO_ADVERTISER);
      checkArgument(_advertiser != null, "Missing OSPF %s", PROP_ADVERTISER);
      RoutingProtocol protocol = _ospfMetricType.toRoutingProtocol();
      switch (protocol) {
        case OSPF_E1:
          return new OspfExternalType1Route(
              getNetwork(),
              getNextHopIp(),
              getAdmin(),
              getMetric(),
              _lsaMetric,
              _area,
              _costToAdvertiser,
              _advertiser,
              getNonForwarding(),
              getNonRouting());
        case OSPF_E2:
          return new OspfExternalType2Route(
              getNetwork(),
              getNextHopIp(),
              getAdmin(),
              getMetric(),
              _lsaMetric,
              _area,
              _costToAdvertiser,
              _advertiser,
              getNonForwarding(),
              getNonRouting());
        default:
          throw new IllegalArgumentException(
              String.format("Invalid OSPF external protocol %s", protocol));
      }
    }

    @Nullable
    public OspfMetricType getOspfMetricType() {
      return _ospfMetricType;
    }

    @Nonnull
    @Override
    protected Builder getThis() {
      return this;
    }

    @Nonnull
    public Builder setAdvertiser(@Nonnull String advertiser) {
      _advertiser = advertiser;
      return getThis();
    }

    @Nonnull
    public Builder setArea(long area) {
      _area = area;
      return getThis();
    }

    @Nonnull
    public Builder setCostToAdvertiser(long costToAdvertiser) {
      _costToAdvertiser = costToAdvertiser;
      return getThis();
    }

    @Nonnull
    public Builder setLsaMetric(long lsaMetric) {
      _lsaMetric = lsaMetric;
      return getThis();
    }

    @Nonnull
    public Builder setOspfMetricType(@Nonnull OspfMetricType ospfMetricType) {
      _ospfMetricType = ospfMetricType;
      return getThis();
    }

    private Builder() {} // only for use by #builder()
  }

  protected static final String PROP_ADVERTISER = "advertiser";
  protected static final String PROP_COST_TO_ADVERTISER = "costToAdvertiser";
  protected static final String PROP_LSA_METRIC = "lsaMetric";

  private static final long serialVersionUID = 1L;

  @Nonnull private final String _advertiser;
  private final long _costToAdvertiser;
  private final long _lsaMetric;

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  OspfExternalRoute(
      Prefix prefix,
      Ip nextHopIp,
      int admin,
      long metric,
      long lsaMetric,
      long area,
      String advertiser,
      long costToAdvertiser,
      boolean nonForwarding,
      boolean nonRouting) {
    super(prefix, nextHopIp, admin, metric, area, nonRouting, nonForwarding);
    _advertiser = advertiser;
    _costToAdvertiser = costToAdvertiser;
    _lsaMetric = lsaMetric;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OspfExternalRoute that = (OspfExternalRoute) o;
    return
    // AbstractRoute properties
    Objects.equals(_network, that._network)
        && _admin == that._admin
        && getNonRouting() == that.getNonRouting()
        && getNonForwarding() == that.getNonForwarding()
        && _metric == that._metric
        && _nextHopIp.equals(that._nextHopIp)
        // OspfRoute properties
        && _area == that._area
        // OspfExternalRoute properties
        && getCostToAdvertiser() == that.getCostToAdvertiser()
        && getLsaMetric() == that.getLsaMetric()
        && Objects.equals(getAdvertiser(), that.getAdvertiser());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        // AbstractRoute properties
        _network,
        _admin,
        _metric,
        _nextHopIp,
        getNonRouting(),
        getNonForwarding(),
        // OspfRoute properties
        _area,
        // OspfExternalRoute properties
        getAdvertiser(),
        getCostToAdvertiser(),
        getLsaMetric());
  }

  @JsonProperty(PROP_ADVERTISER)
  @Nonnull
  public final String getAdvertiser() {
    return _advertiser;
  }

  @JsonProperty(PROP_COST_TO_ADVERTISER)
  public long getCostToAdvertiser() {
    return _costToAdvertiser;
  }

  @JsonProperty(PROP_LSA_METRIC)
  public long getLsaMetric() {
    return _lsaMetric;
  }

  @Nonnull
  @Override
  public String getNextHopInterface() {
    return Route.UNSET_NEXT_HOP_INTERFACE;
  }

  @JsonIgnore
  @Nonnull
  public abstract OspfMetricType getOspfMetricType();

  @Nonnull
  @Override
  public RoutingProtocol getProtocol() {
    return getOspfMetricType().toRoutingProtocol();
  }

  @Override
  public int getTag() {
    return NO_TAG;
  }
}
