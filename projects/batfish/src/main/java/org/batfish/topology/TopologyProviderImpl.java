package org.batfish.topology;

import static org.batfish.datamodel.ospf.OspfTopologyUtils.computeOspfTopology;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.util.IpsecUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.vxlan.VxlanTopology;

@ParametersAreNonnullByDefault
public final class TopologyProviderImpl implements TopologyProvider {
  private static final int MAX_CACHED_SNAPSHOTS = 3;

  private final IBatfish _batfish;
  private final Cache<NetworkSnapshot, IpOwners> _ipOwners;
  private final Cache<NetworkSnapshot, Optional<Layer1Topology>> _layer1LogicalTopologies;
  private final Cache<NetworkSnapshot, Optional<Layer1Topology>> _layer1PhysicalTopologies;
  private final Cache<NetworkSnapshot, Optional<Layer2Topology>> _layer2Topologies;
  private final Cache<NetworkSnapshot, Topology> _layer3Topologies;
  private final Cache<NetworkSnapshot, OspfTopology> _ospfTopologies;
  private final Cache<NetworkSnapshot, Optional<Layer1Topology>> _rawLayer1PhysicalTopologies;
  private final Cache<NetworkSnapshot, Topology> _rawLayer3Topologies;
  private final Cache<NetworkSnapshot, VxlanTopology> _vxlanTopologies;

  /** Create a new topology provider for a given instance of {@link IBatfish} */
  public TopologyProviderImpl(IBatfish batfish) {
    _batfish = batfish;
    _ipOwners = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
    _layer1LogicalTopologies = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
    _layer1PhysicalTopologies = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
    _layer2Topologies =
        CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).softValues().build();
    _layer3Topologies = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
    _ospfTopologies = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
    _rawLayer1PhysicalTopologies =
        CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
    _rawLayer3Topologies = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
    _vxlanTopologies = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_SNAPSHOTS).build();
  }

  private @Nonnull IpOwners computeIpOwners(NetworkSnapshot snapshot) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("TopologyProviderImpl::computeIpOwners").startActive()) {
      assert span != null; // avoid unused warning
      return new IpOwners(_batfish.loadConfigurations(snapshot));
    }
  }

  private @Nonnull Optional<Layer1Topology> computeLayer1LogicalTopology(
      NetworkSnapshot networkSnapshot) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("TopologyProviderImpl::computeLayer1LogicalTopology")
            .startActive()) {
      assert span != null; // avoid unused warning
      return getLayer1PhysicalTopology(networkSnapshot)
          .map(
              layer1PhysicalTopology ->
                  TopologyUtil.computeLayer1LogicalTopology(
                      layer1PhysicalTopology, _batfish.loadConfigurations(networkSnapshot)));
    }
  }

  private @Nonnull Optional<Layer1Topology> computeLayer1PhysicalTopology(
      NetworkSnapshot networkSnapshot) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("TopologyProviderImpl::computeLayer1PhysicalTopology")
            .startActive()) {
      assert span != null; // avoid unused warning
      return getRawLayer1PhysicalTopology(networkSnapshot)
          .map(
              rawLayer1PhysicalTopology ->
                  TopologyUtil.computeLayer1PhysicalTopology(
                      rawLayer1PhysicalTopology, _batfish.loadConfigurations(networkSnapshot)));
    }
  }

  private @Nonnull Optional<Layer2Topology> computeLayer2Topology(NetworkSnapshot networkSnapshot) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("TopologyProviderImpl::computeLayer2Topology").startActive()) {
      assert span != null; // avoid unused warning
      return getLayer1LogicalTopology(networkSnapshot)
          .map(
              layer1LogicalTopology ->
                  TopologyUtil.computeLayer2Topology(
                      layer1LogicalTopology, _batfish.loadConfigurations(networkSnapshot)));
    }
  }

  private Topology computeLayer3Topology(NetworkSnapshot networkSnapshot) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("TopologyProviderImpl::computeLayer3Topology").startActive()) {
      assert span != null; // avoid unused warning
      Map<String, Configuration> configurations = _batfish.loadConfigurations(networkSnapshot);
      Topology topology = getRawLayer3Topology(networkSnapshot);
      return topology.prune(
          Sets.union(
              IpsecUtil.computeFailedIpsecSessionEdges(
                  topology.getEdges(), IpsecUtil.initIpsecTopology(configurations), configurations),
              _batfish.getEdgeBlacklist(networkSnapshot)),
          _batfish.getNodeBlacklist(networkSnapshot),
          _batfish.getInterfaceBlacklist(networkSnapshot));
    }
  }

  private @Nonnull Optional<Layer1Topology> computeRawLayer1PhysicalTopology(
      NetworkSnapshot networkSnapshot) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("TopologyProviderImpl::computeRawLayer1PhysicalTopology")
            .startActive()) {
      assert span != null; // avoid unused warning
      return Optional.ofNullable(_batfish.loadRawLayer1PhysicalTopology(networkSnapshot));
    }
  }

  private Topology computeRawLayer3Topology(NetworkSnapshot networkSnapshot) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("TopologyProviderImpl::computeRawLayer3Topology")
            .startActive()) {
      assert span != null; // avoid unused warning
      Map<String, Configuration> configurations = _batfish.loadConfigurations(networkSnapshot);
      return getRawLayer1PhysicalTopology(networkSnapshot)
          .map(
              rawLayer1PhysicalTopology ->
                  TopologyUtil.computeLayer3Topology(
                      rawLayer1PhysicalTopology,
                      computeLayer2Topology(networkSnapshot).get(),
                      configurations))
          .orElse(TopologyUtil.synthesizeL3Topology(configurations));
    }
  }

  private @Nonnull VxlanTopology computeVxlanTopology(NetworkSnapshot snapshot) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("TopologyProviderImpl::computeVxlanTopology").startActive()) {
      assert span != null; // avoid unused warning
      return new VxlanTopology(_batfish.loadConfigurations(snapshot));
    }
  }

  /** Return the {@link IpOwners} for a given snapshot. */
  @Override
  public IpOwners getIpOwners(NetworkSnapshot snapshot) {
    try {
      return _ipOwners.get(snapshot, () -> computeIpOwners(snapshot));
    } catch (ExecutionException e) {
      return computeIpOwners(snapshot);
    }
  }

  @Override
  public Optional<Layer1Topology> getLayer1LogicalTopology(NetworkSnapshot networkSnapshot) {
    try {
      return _layer1LogicalTopologies.get(
          networkSnapshot, () -> computeLayer1LogicalTopology(networkSnapshot));
    } catch (ExecutionException e) {
      return computeLayer1LogicalTopology(networkSnapshot);
    }
  }

  @Override
  public Optional<Layer1Topology> getLayer1PhysicalTopology(NetworkSnapshot networkSnapshot) {
    try {
      return _layer1PhysicalTopologies.get(
          networkSnapshot, () -> computeLayer1PhysicalTopology(networkSnapshot));
    } catch (ExecutionException e) {
      return computeLayer1PhysicalTopology(networkSnapshot);
    }
  }

  @Override
  public Optional<Layer2Topology> getLayer2Topology(NetworkSnapshot networkSnapshot) {
    try {
      return _layer2Topologies.get(networkSnapshot, () -> computeLayer2Topology(networkSnapshot));
    } catch (ExecutionException e) {
      return computeLayer2Topology(networkSnapshot);
    }
  }

  @Override
  public Topology getLayer3Topology(NetworkSnapshot networkSnapshot) {
    try {
      return _layer3Topologies.get(networkSnapshot, () -> computeLayer3Topology(networkSnapshot));
    } catch (ExecutionException e) {
      return computeLayer3Topology(networkSnapshot);
    }
  }

  @Override
  public OspfTopology getOspfTopology(NetworkSnapshot networkSnapshot) {
    try {
      return _ospfTopologies.get(
          networkSnapshot,
          () ->
              computeOspfTopology(
                  NetworkConfigurations.of(_batfish.loadConfigurations(networkSnapshot)),
                  getLayer3Topology(networkSnapshot)));
    } catch (ExecutionException e) {
      return computeOspfTopology(
          NetworkConfigurations.of(_batfish.loadConfigurations(networkSnapshot)),
          getLayer3Topology(networkSnapshot));
    }
  }

  @Override
  public Optional<Layer1Topology> getRawLayer1PhysicalTopology(NetworkSnapshot networkSnapshot) {
    try {
      return _rawLayer1PhysicalTopologies.get(
          networkSnapshot, () -> computeRawLayer1PhysicalTopology(networkSnapshot));
    } catch (ExecutionException e) {
      return computeRawLayer1PhysicalTopology(networkSnapshot);
    }
  }

  @Override
  public Topology getRawLayer3Topology(NetworkSnapshot networkSnapshot) {
    try {
      return _rawLayer3Topologies.get(
          networkSnapshot, () -> computeRawLayer3Topology(networkSnapshot));
    } catch (ExecutionException e) {
      return computeRawLayer3Topology(networkSnapshot);
    }
  }

  @Override
  public VxlanTopology getVxlanTopology(NetworkSnapshot snapshot) {
    try {
      return _vxlanTopologies.get(snapshot, () -> computeVxlanTopology(snapshot));
    } catch (ExecutionException e) {
      return computeVxlanTopology(snapshot);
    }
  }
}
