package com.jam.module.topic.controller;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jam.common.BaseController;
import com.jam.javautils.string.StringUtil;
import com.jam.module.collect.service.CollectService;
import com.jam.module.common.CscSysUtils;
import com.jam.module.reply.entity.Reply;
import com.jam.module.reply.service.ReplyService;
import com.jam.module.security.service.RoleService;
import com.jam.module.topic.entity.Topic;
import com.jam.module.topic.service.TopicService;
import com.jam.module.user.entity.User;
import com.jam.module.user.service.UserService;

/**
 * Created by eclipse. Copyright (c) 2016, All Rights Reserved.
 */
@Controller
@RequestMapping("/topic")
public class TopicController extends BaseController {
	@Autowired
	private TopicService topicService;
	@Autowired
	private ReplyService replyService;
	@Autowired
	private CollectService collectService;
	@Autowired
	private UserService userService;
	@Autowired
	private RoleService roleService;

	/**
	 * 创建话题
	 *
	 * @return
	 */
	@RequestMapping("/create")
	public String create() {
		return render("/topic/create");
	}

	/**
	 * 保存话题
	 *
	 * @param title
	 * @param content
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String save(String tab, String title, String content, Model model, HttpServletResponse response) {
		String errors = "";
		if (StringUtil.isBlank(title)) {
			errors = "标题不能为空";
		} else if (StringUtil.isBlank(tab)) {
			errors = "版块不能为空";
		} else {
			User user = getUser();
			Topic topic = new Topic();
			topic.setTab(tab);
			topic.setTitle(title);
			topic.setContent(content);
			topic.setInTime(new Date());
			topic.setUp(0);
			topic.setView(0);
			topic.setUser(user);
			topic.setGood(false);
			topic.setTop(false);
			topicService.save(topic);
			return redirect(response, "/topic/" + topic.getId());
		}
		model.addAttribute("errors", errors);
		return render("/topic/create");
	}

	@RequestMapping("/{id}/edit")
	public String edit(@PathVariable int id, HttpServletResponse response, Model model) {
		Topic topic = topicService.findById(id);
		if (topic == null) {
			renderText(response, "话题不存在");
			return null;
		} else {
			model.addAttribute("topic", topic);
			return render("/topic/edit");
		}
	}

	/**
	 * 更新话题
	 *
	 * @param title
	 * @param content
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/{id}/edit", method = RequestMethod.POST)
	public String update(@PathVariable Integer id, String tab, String title, String content, Model model,
			HttpServletResponse response) {
		Topic topic = topicService.findById(id);
		User user = getUser();
		if (topic.getUser().getId() == user.getId()) {
			topic.setTab(tab);
			topic.setTitle(title);
			topic.setContent(content);
			topicService.save(topic);
			return redirect(response, "/topic/" + topic.getId());
		} else {
			renderText(response, "非法操作");
			return null;
		}
	}

	/**
	 * 话题详情
	 *
	 * @param id
	 * @param model
	 * @return
	 */
	@RequestMapping("/{id}")
	public String detail(@PathVariable Integer id, HttpServletResponse response, Model model) {
		if (id != null) {
			Topic topic = topicService.findById(id);
			List<Reply> replies = replyService.findByTopicId(id);
			replies = CscSysUtils.getPrivateReplies(userService, getUser(), getSiteConfig(), topic, replies);
			model.addAttribute("topic", topic);
			model.addAttribute("replies", replies);
			model.addAttribute("user", getUser());
			model.addAttribute("author", topic.getUser());
			model.addAttribute("otherTopics", topicService.findByUser(1, 7, topic.getUser()));
			model.addAttribute("collect", collectService.findByUserAndTopic(getUser(), topic));
			model.addAttribute("collectCount", collectService.countByTopic(topic));
			return render("/topic/detail");
		} else {
			renderText(response, "话题不存在");
			return null;
		}
	}

	/**
	 * 删除话题
	 *
	 * @param id
	 * @return
	 */
	@RequestMapping("/{id}/delete")
	public String delete(@PathVariable Integer id, HttpServletResponse response) {
		if (id != null) {
			topicService.deleteById(id);
		}
		return redirect(response, "/");
	}
}
