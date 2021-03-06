package cn.edu.hut.crazyacking.spider;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import cn.edu.hut.crazyacking.spider.utils.Constants;
import cn.edu.hut.crazyacking.spider.utils.DBConn;
import cn.edu.hut.crazyacking.spider.utils.Utils;
import cn.edu.hut.crazyacking.spider.worker.impl.UrlAbnormalWeiboWorker;
import cn.edu.hut.crazyacking.spider.worker.impl.UrlCommentWorker;
import cn.edu.hut.crazyacking.spider.worker.impl.UrlFollowWorker;
import cn.edu.hut.crazyacking.spider.worker.impl.UrlRepostWorker;
import cn.edu.hut.crazyacking.spider.worker.impl.UrlWeiboWorker;
import cn.edu.hut.crazyacking.spider.parser.WebDataExtraction;
import cn.edu.hut.crazyacking.spider.storage.PageStorage;

/*
* -----------------------------------------------------------------
* Copyright (c) 2015 crazyacking All rights reserved.
* -----------------------------------------------------------------
*       Author: crazyacking
*       Submission Date: 2015/3/25
*/

public class WeiboSpiderStarter {
    private static final Logger Log = Logger.getLogger(WeiboSpiderStarter.class.getName());
    private static int WORKER_NUM = 1;
    private static String TYPE;

    public static void main(String[] args) throws IOException, InterruptedException {

        // 初始化配置参数
        initializeParams();

        // 根据type判断爬虫任务类型
        if (TYPE.equals("weibo")) {
            fetchWeibo();
        } else if (TYPE.equals("comment")) {
            fetchComment();
        } else if (TYPE.equals("repost")) {
            fetchRepost();
        } else if (TYPE.equals("abnormal")) {
            fetchAbnormalWeibo();
        } else if (TYPE.equals("follow")) {
            fetchFollowee();
        } else {
            Log.error("Unknown crawl type: " + TYPE + ".\n Exit...");
        }


        PageStorage pageStorage = new PageStorage();
        pageStorage.WebPageStorage();

    }

    /**
     * 从配置文件中读取配置信息：数据库连接、相关文件根目录、爬虫任务类型
     */
    private static void initializeParams() {
        InputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream("conf\\spider.properties"));
            Properties properties = new Properties();
            properties.load(in);

            // 从配置文件中读取数据库连接参数
            DBConn.CONN_URL = properties.getProperty("DB.connUrl");
            DBConn.DB_NAME = properties.getProperty("DB.name");
            DBConn.USERNAME = properties.getProperty("DB.username");
            DBConn.PASSWORD = properties.getProperty("DB.password");

            // 从配置文件中读取根目录，并设置相关文件地址
            Constants.ROOT_DISK = properties.getProperty("spider.rootDisk");
            Constants.REPOST_LOG_PATH = Constants.ROOT_DISK + "repost_log.txt";
            Constants.COMMENT_LOG_PATH = Constants.ROOT_DISK + "comment_log.txt";
            Constants.SWITCH_ACCOUNT_LOG_PATH = Constants.ROOT_DISK + "switch_account_log.txt";
            Constants.ACCOUNT_PATH = Constants.ROOT_DISK + "account.txt";
            Constants.ACCOUNT_RESULT_PATH = Constants.ROOT_DISK + "account_result.txt";
            Constants.LOGIN_ACCOUNT_PATH = Constants.ROOT_DISK + "login_account.txt";
            Constants.ABNORMAL_ACCOUNT_PATH = Constants.ROOT_DISK + "abnormal_account.txt";
            Constants.ABNORMAL_WEIBO_PATH = Constants.ROOT_DISK + "abnormal_weibo.txt";
            Constants.ABNORMAL_WEIBO_CLEANED_PATH = Constants.ROOT_DISK + "abnormal_weibo_cleaned.txt";

            // 从配置文件中读取爬虫任务类型
            WeiboSpiderStarter.TYPE = properties.getProperty("spider.type");

            // 从配置文件中读取follow爬取相关参数
            if (TYPE.equals("follow")) {
                Constants.LEVEL = Integer.parseInt(properties.getProperty("follow.level"));
                Constants.FANS_NO_MORE_THAN = Integer.parseInt(properties.getProperty("follow.maxFansNum"));
            }

            // 从配置文件中读取微博相关参数
            Constants.CHECK_WEIBO_NUM = Boolean.parseBoolean(properties.getProperty("weibo.checkWeiboNum", "false"));
            if (Constants.CHECK_WEIBO_NUM) {
                Constants.WEIBO_NO_MORE_THAN = Integer.parseInt(properties.getProperty("weibo.maxWeiboNum"));
            }

            in.close();
        } catch (FileNotFoundException e) {
            Log.error(e);
        } catch (IOException e) {
            Log.error(e);
        }
    }

    private static void fetchWeibo() {
        Log.info("\n\n\n===========================================================\n\t\t基于网络爬虫的好友推荐系统\n\t\t\t\t ----------------------zeekEye    \n===========================================================\n");

        // 初始化账号队列
        /*
        * 把文件中的账号读入到AccountQueue中
		* */
        Utils.readAccountFromFile();

        // 初始化微博页面链接
        /*
        * 从数据库中取出待爬取用户ID，构造初始用户粉丝页面的Url，并放入待爬取队列WeiboUrlQueue
		* */
        Utils.initializeWeiboUrl();

        // 启动爬虫worker线程
        for (int i = 0; i < WORKER_NUM; i++) {
            new Thread(new UrlWeiboWorker()).start();
        }
    }

    private static void fetchAbnormalWeibo() {
        Log.info("\n\n\n===========================\n     Abnormal Weibo\n===========================\n");
        // 初始化账号队列
        Utils.readAccountFromFile();

        // 初始化微博页面链接
        Utils.initializeAbnormalWeiboUrl();

        // 启动爬虫worker线程
        for (int i = 0; i < WORKER_NUM; i++) {
            new Thread(new UrlAbnormalWeiboWorker()).start();
        }
    }

    private static void fetchComment() {
        Log.info("\n\n\n===========================\n     Fetch Comment\n===========================\n");
        // 初始化账号队列
        Utils.readAccountFromFile();

        // 初始化评论页面链接
        Utils.initializeCommentUrl();

        // 启动爬虫worker线程
        for (int i = 0; i < WORKER_NUM; i++) {
            new Thread(new UrlCommentWorker()).start();
        }
    }

    private static void fetchRepost() {
        Log.info("\n\n\n===========================\n     Fetch Repost\n===========================\n");
        // 初始化账号队列
        Utils.readAccountFromFile();

        // 初始化转发页面链接
        Utils.initializeRepostUrl();

        // 启动爬虫worker线程
        for (int i = 0; i < WORKER_NUM; i++) {
            new Thread(new UrlRepostWorker()).start();
        }
    }

    private static void fetchFollowee() {
        Log.info("\n\n\n===========================\n     Fetch Followee\n===========================\n");
        // 初始化账号队列
        Utils.readAccountFromFile();

        // 初始化关注页面链接
        UrlFollowWorker.CURRENT_LEVEL = Utils.initializeFollowUrl();

        // 启动爬虫worker线程
        for (int i = 0; i < WORKER_NUM; i++) {
            new Thread(new UrlFollowWorker()).start();
        }
    }
}
