package com.d201.fundingift.friend.domain.port;

import com.d201.fundingift.friend.dto.GetFriendCommand;

import java.util.List;

public interface FriendExternalPort {

    List<GetFriendCommand> getFriends(Long consumerId); // 친구 목록 불러오기
}
