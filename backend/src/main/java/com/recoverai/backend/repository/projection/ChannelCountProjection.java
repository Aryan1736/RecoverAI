package com.recoverai.backend.repository.projection;

import com.recoverai.backend.entity.enums.RecoveryChannel;

public interface ChannelCountProjection {
    RecoveryChannel getChannel();
    Long getCount();
}
