package com.johu.spider.spider.task;

import com.johu.spider.spider.config.HttpClientDownloaderConfig;
import com.johu.spider.spider.entity.Porn91VideoList;
import com.johu.spider.spider.mapper.Porn91VideoListMapper;
import com.johu.spider.spider.spider.pipeline.Porn91InitPipeline;
import com.johu.spider.spider.spider.pipeline.Porn91InitPipeline1;
import com.johu.spider.spider.spider.processor.Porn91CoreProcessor;
import com.johu.spider.spider.spider.processor.Porn91InitProcessor;
import com.johu.spider.spider.spider.processor.Porn91PageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.HttpClientDownloader;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author wennan
 * 2018/1/6
 */

@Component
public class Download91PornInit implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private Porn91InitProcessor porn91InitProcessor;

    @Autowired
    private Porn91PageProcessor porn91PageProcessor;


    @Autowired
    private Porn91VideoListMapper porn91VideoListMapper;

    @Autowired
    private HttpClientDownloader httpClientDownloader;

    @Autowired
    private Porn91InitPipeline porn91InitPipeline;

    @Autowired
    private Porn91InitPipeline1 porn91InitPipeline1;

    @Autowired
    private Executor initThreadPool;

    @Autowired
    private Download91PornCore download91PornCore;

    private final String SUFFIX = "&page=";

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        List<Porn91VideoList> porn91VideoListList = porn91VideoListMapper.selectAll();
        String[] urlArray = porn91VideoListList.stream().map(Porn91VideoList::getUrl).toArray(String[]::new);
        if(urlArray.length==0){
            return;
        }

//        更新最大页数
//        Spider.create(porn91InitProcessor)
//                .addUrl(urlArray)
//                .setDownloader(httpClientDownloader)
//                .addPipeline(porn91InitPipeline)
//                .run();

        //更新爬取初始页
        porn91VideoListList.stream().filter(x->x.getPageNo()<=x.getMaxPageNo()).forEach(x->CompletableFuture.runAsync(()->this.initDownloadUrl(x),initThreadPool));

        //获取视频动态地址，并下载视频
        download91PornCore.run();

    }

    //更新爬取初始页
    public void initDownloadUrl(Porn91VideoList porn91VideoList){
        String finalUrl = Optional.ofNullable(porn91VideoList).map(x->x.getUrl()+SUFFIX+x.getPageNo()).orElse("");
        String url = porn91VideoList.getUrl();
        Integer pageNo = porn91VideoList.getPageNo();
        Integer maxPageNo = porn91VideoList.getMaxPageNo();
        if(StringUtils.isEmpty(finalUrl)) return ;
        while (true){
            if(pageNo>maxPageNo) break;
            Spider.create(porn91PageProcessor)
                    .addUrl(finalUrl)
                    .setDownloader(httpClientDownloader)
                    .addPipeline(porn91InitPipeline1)
                    .run();

            pageNo++;
            Porn91VideoList updateEntity = new Porn91VideoList();
            updateEntity.setId(porn91VideoList.getId());
            finalUrl = url+SUFFIX+pageNo;
            updateEntity.setPageNo(pageNo);
            porn91VideoListMapper.updatePageNo(updateEntity);
        }


    }


}
