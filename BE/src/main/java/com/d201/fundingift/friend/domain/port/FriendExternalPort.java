package com.d201.fundingift.friend.domain.port;

import com.d201.fundingift.friend.dto.response.GetFriendResponse;

import java.util.List;

public interface FriendExternalPort {

    List<GetFriendResponse> getFriends(Long consumerId); // 친구 목록 불러오기
}
