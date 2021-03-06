package org.batfish.datamodel.acl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AclTrace implements Serializable {

  private static final String PROP_EVENTS = "events";

  private static final long serialVersionUID = 1L;

  private final List<TraceEvent> _events;

  @JsonCreator
  public AclTrace(@JsonProperty(PROP_EVENTS) @Nullable Iterable<TraceEvent> events) {
    _events = events != null ? ImmutableList.copyOf(events) : ImmutableList.of();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AclTrace)) {
      return false;
    }
    return _events.equals(((AclTrace) obj)._events);
  }

  @JsonProperty(PROP_EVENTS)
  public @Nonnull List<TraceEvent> getEvents() {
    return _events;
  }

  @Override
  public int hashCode() {
    return _events.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add(PROP_EVENTS, _events).toString();
  }
}
