package cn.edu.hut.crazyacking.spider.worker.impl;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import cn.edu.hut.crazyacking.spider.fetcher.WeiboFetcher;
import cn.edu.hut.crazyacking.spider.handler.NextUrlHandler;
import cn.edu.hut.crazyacking.spider.parser.WeiboParser;
import cn.edu.hut.crazyacking.spider.parser.bean.Account;
import cn.edu.hut.crazyacking.spider.queue.AccountQueue;
import cn.edu.hut.crazyacking.spider.queue.WeiboUrlQueue;
import cn.edu.hut.crazyacking.spider.utils.Utils;
import cn.edu.hut.crazyacking.spider.worker.BasicWorker;

/**
 * 从UrlQueue中取出url，下载页面，分析url，保存已访问rul
 *author:crazyacking
 */
public class UrlWeiboWorker extends BasicWorker implements Runnable {
    private static final Logger Log = Logger.getLogger(UrlWeiboWorker.class.getName());

    /**
     * 下载对应页面并分析出页面对应URL，放置在未访问队列中
     *
     * @param url 返回值：被封账号/系统繁忙/OK
     */
    protected String dataHandler(String url) {
        return NextUrlHandler.addNextWeiboUrl(WeiboFetcher.getContentFromUrl(url));
    }

    @Override
    public void run() {
        // 首先获取账号并登录
        System.out.println("启动爬虫线程...");
        //获取一个登录账号
        Account account = AccountQueue.outElement();
        AccountQueue.addElement(account);
        this.username = account.getUsername();
        this.password = account.getPassword();

        // 使用账号登录，并获取gsid

        String gsid = login(username, password);
        String result = null;
        try {
            // 若登录失败，则执行一轮切换账户的操作，如果还失败，则退出
            if (gsid == null) {
                gsid = switchAccount();
                System.out.println("微博登录失败，正在切换账号...");
                Thread.sleep(1000);
            }

            // 登录成功
            if (gsid != null) {
                System.out.println("微博登录成功，开始获取gsid码...");
                Thread.sleep(1000);
                // 当URL队列不为空时，从未访问队列中取出url进行分析
                while (!WeiboUrlQueue.isEmpty()) {
                    // 从队列中获取URL并处理
                    result = dataHandler(WeiboUrlQueue.outElement() + "&" + gsid);
                    System.out.println("System " + result + ".");

                    // 针对处理结果进行处理：OK, SYSTEM_BUSY, ACCOUNT_FORBIDDEN
                    gsid = process(result, gsid);

                    System.out.println(gsid);

                    // 没有新的URL了，从数据库中继续拿一个
                    if (WeiboUrlQueue.isEmpty()) {
                        // 仍为空，从数据库中取
                        if (WeiboUrlQueue.isEmpty()) {
                            System.out.println(">> Add new weibo Url...");
                            Log.info(">> Add new weibo Url...");
                            Utils.initializeWeiboUrl();

                            // 拿完还是空，退出爬虫
                            if (WeiboUrlQueue.isEmpty()) {
                                System.out.println(">> All users have been fetched...");
                                Log.info(">> All users have been fetched...");
                                break;
                            }
                        }
                    }
                }
            } else {
                System.out.println(">> " + username + " login failed!");
                Log.info(">> " + username + " login failed!");
            }

        } catch (InterruptedException e) {
            Log.error(e);
        } catch (IOException e) {
            Log.error(e);
        }

        // 关闭数据库连接
        try {
            WeiboParser.conn.close();
            Utils.conn.close();
        } catch (SQLException e) {
            Log.error(e);
        }
        System.out.println("Spider stop...");
        Log.info("Spider stop...");
    }

}
