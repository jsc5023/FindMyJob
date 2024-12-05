package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // 사용할 크롤링 함수 선택
        crawlJobKorea();
        crawlSaramin();
    }

    public static void crawlJobKorea() {
        String baseUrl = "https://www.jobkorea.co.kr/Search/?stext=Java&careerType=1&careerMax=2&tabType=recruit&Page_No=";
        int maxPages = 5; // 검색할 최대 페이지 수 (필요에 따라 조정)
        int delay = 2000; // 요청 간격 (ms)

        List<JobPost> jobList = new ArrayList<>();

        try {
            for (int page = 1; page <= maxPages; page++) {
                String url = baseUrl + page;

                System.out.println("현재 페이지: " + page);

                // Jsoup으로 HTML 가져오기
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36")
                        .timeout(5000)
                        .get();

                // 공고 리스트 가져오기
                Elements jobPosts = doc.select(".list-item");

                if (jobPosts.isEmpty()) {
                    System.out.println("더 이상 데이터가 없습니다.");
                    break;
                }

                // 공고 데이터 출력
                for (Element post : jobPosts) {
                    String title = post.select(".list-section-information .information-title-link").text(); // 제목
                    String company = post.select(".list-section-corp .corp-name-link").text(); // 회사명
                    String link = post.select(".list-section-information .information-title-link").attr("href"); // 상세 링크
                    link = "https://www.jobkorea.co.kr" + link;
                    String location = post.select(".chip-information-group .chip-information-item:nth-child(4)").text();

                    // SI/SM 필터링
                    if (!title.isEmpty()) {
                        jobList.add(new JobPost(title, company, location, link));
                        System.out.println("제목: " + title);
                        System.out.println("회사: " + company);
                        System.out.println("링크: " + link);
                        System.out.println("--------------------------------------");
                    }
                }

                // 요청 간격 설정
                Thread.sleep(delay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        saveJobsToFile(jobList, "jobKorea.txt");
    }

    public static void crawlSaramin() {
        String baseUrl = "https://www.saramin.co.kr/zf_user/search/recruit";
        int page = 1;
        List<JobPost> jobList = new ArrayList<>();

        try {
            while (true) {
                String url = baseUrl + "?searchType=search&exp_cd=1,2&exp_max=2&company_cd=0,1,2,3,4,5,6,7,9,10" +
                        "&searchword=Java&recruitPageCount=40&recruitPage=" + page;
                Document doc = Jsoup.connect(url).header("Content-Type", "text/html; charset=UTF-16")
                        .get();
                Elements jobPosts = doc.select(".item_recruit");

                if (jobPosts.isEmpty()) {
                    break;
                }

                for (Element post : jobPosts) {
                    try {
                        Element titleElement = post.selectFirst(".job_tit a");
                        Element companyElement = post.selectFirst(".corp_name");
                        Element linkElement = post.selectFirst(".job_tit a");

                        String title = (titleElement != null) ? titleElement.text() : "no title";
                        String company = (companyElement != null) ? companyElement.text() : "no company";
                        String link = (linkElement != null) ? linkElement.attr("href") : "no link";

                        if (!containsExcludeKeywords(title)) {
                            jobList.add(new JobPost(title, company, "", link));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                page++;

                if (page > 50) {
                    break;
                }
            }

            // 결과 출력
            saveJobsToFile(jobList, "saramin.txt");

        } catch (IOException e) {
            System.out.println("Error : " + e.getMessage());
        }
    }

    private static boolean containsExcludeKeywords(String text) {
        String[] excludeKeywords = {};
        // String[] excludeKeywords = {"SI", "SM"} 만약 SI, SM을 원하지 않는 경우 이런식으로 사용하면 됩니다.
        for (String keyword : excludeKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static void saveJobsToFile(List<JobPost> jobList, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (JobPost job : jobList) {
                writer.write(job.toString());
                writer.newLine(); // 줄바꿈
            }
            System.out.println("Job posts saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save job posts to file: " + e.getMessage());
        }
    }
}

class JobPost {
    private String title;
    private String company;
    private String location;
    private String link;

    public JobPost(String title, String company, String location, String link) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.link = link;
    }

    @Override
    public String toString() {
        return String.format("Title: %s\nCompany: %s\nLocation: %s\nLink: %s\n", title, company, location, link);
    }
}
