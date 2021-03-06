package com.jam.module.notification.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jam.module.notification.entity.Notification;
import com.jam.module.topic.entity.Topic;
import com.jam.module.user.entity.User;

/**
 * Created by eclipse. Copyright (c) 2016, All Rights Reserved.
 */
@Repository
public interface NotificationDao extends JpaRepository<Notification, Integer> {

	Page<Notification> findByTargetUser(User targetUser, Pageable pageable);

	Page<Notification> findByTargetUserAndIsRead(User targetUser, boolean isRead, Pageable pageable);

	List<Notification> findByTargetUserAndIsRead(User targetUser, boolean isRead);

	long countByTargetUserAndIsRead(User targetUser, boolean isRead);

	@Modifying
	@Query("update Notification n set n.isRead = true where n.targetUser = ?1")
	void updateByIsRead(User targetUser);
	
	void deleteByTopicId(int topicId);

}
