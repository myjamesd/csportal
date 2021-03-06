package com.jam.module.index.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.jam.common.BaseController;
import com.jam.common.config.SiteConfig;
import com.jam.javautils.string.StringUtil;
import com.jam.module.topic.elastic.ElasticTopicService;
import com.jam.module.topic.entity.Topic;
import com.jam.module.topic.service.TopicService;
import com.jam.module.user.entity.User;
import com.jam.module.user.service.UserService;
import com.jam.util.JsonUtil;
import com.jam.util.identicon.Identicon;

import lombok.extern.log4j.Log4j;

/**
 * Created by eclipse. Copyright (c) 2016, All Rights Reserved.
 */
@Controller
@Log4j
public class IndexController extends BaseController {

	@Autowired
	private TopicService topicService;
	@Autowired
	private UserService userService;
	@Autowired
	private SiteConfig siteConfig;
	@Autowired
	private Identicon identicon;
	@Autowired
	private ElasticTopicService elasticTopicService;

	/**
	 * 首页
	 *
	 * @return
	 */
	@RequestMapping("/")
	public String index(String tab, Integer p, Model model) {
		String sectionName = tab;
		if (StringUtil.isBlank(tab))
			tab = "全部";
		if (tab.equals("全部") || tab.equals("精华") || tab.equals("等待回复")) {
			sectionName = "版块";
		}
		Page<Topic> page = topicService.page(p == null ? 1 : p, siteConfig.getPageSize(), tab);
		model.addAttribute("page", page);
		model.addAttribute("tab", tab);
		model.addAttribute("sectionName", sectionName);
		model.addAttribute("user", getUser());
		return render("/index");
	}

	/**
	 * 进入登录页
	 *
	 * @return
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String toLogin(String s, Model model, HttpServletResponse response) {
		if (getUser() != null) {
			return redirect(response, "/");
		}
		model.addAttribute("s", s);
		return render("/login");
	}

	/**
	 * 进入注册页面
	 *
	 * @return
	 */
	@RequestMapping(value = "/register", method = RequestMethod.GET)
	public String toRegister(HttpServletResponse response) {
		if (getUser() != null) {
			return redirect(response, "/");
		}
		return render("/register");
	}

	/**
	 * 进入密码修改页面
	 *
	 * @return
	 */
	@RequestMapping(value = "/modify", method = RequestMethod.GET)
	public String toModify(Model model, HttpServletResponse response) {
		if (getUser() == null) {
			return redirect(response, "/");
		}
		model.addAttribute("user", getUser());
		return render("/modify");
	}

	/**
	 * 注册验证
	 *
	 * @param username
	 * @param password
	 * @return
	 */
	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public String register(String username, String password, String passwordcheck, HttpServletResponse response,
			HttpServletRequest request, Model model) {
		User user = userService.findByUsername(username);
		if (user != null) {
			model.addAttribute("errors", "用户名已经被注册");
		} else if (StringUtil.isBlank(username)) {
			model.addAttribute("errors", "用户名不能为空");
		} else if (StringUtil.isBlank(password)) {
			model.addAttribute("errors", "密码不能为空");
		} else if (StringUtil.isBlank(passwordcheck)) {
			model.addAttribute("errors", "再次输入密码不能为空");
		} else if (!password.equals(passwordcheck)) {
			model.addAttribute("errors", "两次输入的密码不一致");
		} else {
			String avatarName = StringUtil.getUUID();
			identicon.generator(avatarName);
			user = new User();
			user.setUsername(username);
			user.setPassword(new BCryptPasswordEncoder().encode(password));
			user.setInTime(new Date());
			user.setAvatar(siteConfig.getBaseUrl() + "static/images/avatar/" + avatarName + ".png");
			userService.save(user);
			HttpSession session = request.getSession();
			session.removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
			return redirect(response, "/login?s=reg");
		}
		return render("/register");
	}

	/**
	 * 修改密码验证
	 *
	 * @param username
	 * @param password
	 * @return
	 */
	@RequestMapping(value = "/modify", method = RequestMethod.POST)
	public String modify(String password, String passwordnew, String passwordnewcheck, HttpServletResponse response,
			Model model) {
		User u = getUser();
		if (u == null) {
			return redirect(response, "/");
		}
		String userpasswd = u.getPassword();
		if (StringUtil.isBlank(password)) {
			model.addAttribute("errors", "原密码不能为空");
		} else if (StringUtil.isBlank(passwordnew)) {
			model.addAttribute("errors", "新密码不能为空");
		} else if (StringUtil.isBlank(passwordnewcheck)) {
			model.addAttribute("errors", "再次输入新密码不能为空");
		} else if (!passwordnew.equals(passwordnewcheck)) {
			model.addAttribute("errors", "两次输入的新密码不一致");
		} else if (!new BCryptPasswordEncoder().matches(password, userpasswd)) {
			model.addAttribute("errors", "原密码输入错误");
		} else {
			u.setPassword(new BCryptPasswordEncoder().encode(passwordnew));
			userService.save(u);
			destoryPrincipal();
			return redirect(response, "/login");
		}

		model.addAttribute("user", getUser());
		return render("/modify");
	}

	/**
	 * 上传
	 * 
	 * @param file
	 * @return
	 */
	@RequestMapping("/upload")
	@ResponseBody
	public String upload(@RequestParam("file") MultipartFile file) {
		if (!file.isEmpty()) {
			try {
				String dir = siteConfig.getUploadPath();
				if (StringUtil.isBlank(dir)) {
					throw new RuntimeException("文件上传目录为空，请检查配置文件config.yml");
				}

				File fdir = new File(dir);
				if (!fdir.exists()) {
					log.info("目录不存在，创建目录：dir:" + dir);
					fdir.mkdirs();
				}

				String type = file.getContentType();
				String suffix = "." + type.split("/")[1];
				String fileName = StringUtil.getUUID() + suffix;
				byte[] bytes = file.getBytes();
				BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(dir + fileName)));
				stream.write(bytes);
				stream.close();
				return JsonUtil.success(siteConfig.getBaseUrl() + "static/images/upload/" + fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return JsonUtil.error("上传失败");
	}

	/**
	 * 搜索
	 * 
	 * @param p
	 * @param q
	 * @param model
	 * @return
	 */
	@RequestMapping("/search")
	public String search(Integer p, String q, Model model) {
		model.addAttribute("q", q);
		model.addAttribute("page", elasticTopicService.pageByKeyword(p == null ? 1 : p, siteConfig.getPageSize(), q));
		return render("/search");
	}

	/**
	 * 查看协议
	 *
	 * @return
	 */
	@RequestMapping("/protocol")
	public String protocol(HttpServletResponse response) {
		return render("/protocol");
	}

}
