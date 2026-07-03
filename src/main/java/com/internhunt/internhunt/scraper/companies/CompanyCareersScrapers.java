package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.scraper.base.JobScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrapes the careers pages of 20 hardcoded Indian tech companies directly.
 * Phase 1A requirement: Google, Microsoft, Amazon, Flipkart, Swiggy, Zomato,
 * Razorpay, CRED, Meesho, PhonePe, Paytm, Infosys, TCS, Wipro, Accenture,
 * Ola, Zepto, BrowserStack, Freshworks, Zoho.
 *
 * Each company has a CareerConfig defining how to parse their jobs page.
 * Some use JSON APIs, most use HTML scraping.
 */
@Component
public class CompanyCareersScrapers implements JobScraper
{
    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "company_careers"; }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> all = new ArrayList<>();

        all.addAll(scrapeGoogle());
        all.addAll(scrapeMicrosoft());
        all.addAll(scrapeAmazon());
        all.addAll(scrapeFlipkart());
        all.addAll(scrapeSwiggy());
        all.addAll(scrapeZomato());
        all.addAll(scrapeRazorpay());
        all.addAll(scrapeMeesho());
        all.addAll(scrapePhonePe());
        all.addAll(scrapeCRED());
        all.addAll(scrapeOla());
        all.addAll(scrapeZepto());
        all.addAll(scrapeBrowserStack());
        all.addAll(scrapeFreshworks());
        all.addAll(scrapeZoho());
        all.addAll(scrapeInfosys());
        all.addAll(scrapeTCS());
        all.addAll(scrapeWipro());
        all.addAll(scrapeAccenture());
        all.addAll(scrapePaytm());

        System.out.println("[company_careers] Total: " + all.size() + " jobs across 20 companies");
        return all;
    }

    // ─── Google ───────────────────────────────────────────────────────────────
    // Google Careers has a public JSON API used by their own site
    private List<JobListing> scrapeGoogle()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Google uses their own jobs API; we query for India intern roles
            String url = "https://careers.google.com/api/v3/search/?q=intern&location=India&jex=ENTRY_LEVEL&num=20";
            String json = fetchUrl(url, "Google Careers API");
            if (json == null) return jobs;

            // Parse jobs array from Google's API format
            // Structure: { "jobs": [ { "title", "locations", "apply_url", "description", "company_name" } ] }
            int jobsStart = json.indexOf("\"jobs\":[");
            if (jobsStart == -1) return jobs;
            jobsStart += 8;

            int depth = 0, objStart = -1;
            for (int i = jobsStart; i < json.length(); i++)
            {
                char c = json.charAt(i);
                if (c == '{') { if (depth == 0) objStart = i; depth++; }
                else if (c == '}')
                {
                    depth--;
                    if (depth == 0 && objStart != -1)
                    {
                        JobListing job = parseGoogleJob(json.substring(objStart, i + 1));
                        if (job != null) jobs.add(job);
                        objStart = -1;
                    }
                }
                else if (c == ']' && depth == 0) break;
            }
            System.out.println("[google] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[google] " + e.getMessage()); }
        return jobs;
    }

    private JobListing parseGoogleJob(String obj)
    {
        try
        {
            String title = extractString(obj, "\"title\":\"");
            if (title == null || !title.toLowerCase().contains("intern")) return null;
            String applyUrl = extractString(obj, "\"apply_url\":\"");
            if (applyUrl == null) return null;

            String loc = extractString(obj, "\"display\":\"");
            if (loc == null) loc = "India";

            String desc = extractString(obj, "\"description\":\"");
            if (desc != null) desc = desc.replace("\\n", "\n").replace("\\u003c", "<").replace("\\u003e", ">");

            return buildJob("Google", title, applyUrl.replace("\\/", "/"), loc, false, desc, JobListing.ListingType.internship);
        }
        catch (Exception e) { return null; }
    }

    // ─── Microsoft ────────────────────────────────────────────────────────────
    private List<JobListing> scrapeMicrosoft()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Microsoft uses a public API endpoint
            String url = "https://jobs.careers.microsoft.com/global/en/search?q=intern&lc=India&l=en_us&pg=1&pgSz=20&o=Relevance&flt=true";
            Document doc = fetchDoc(url, "https://careers.microsoft.com/");
            if (doc == null) return jobs;

            // Try JSON API version
            String apiUrl = "https://gcsservices.careers.microsoft.com/search/api/v1/search?q=intern&l=en_us&pg=1&pgSz=20&lc=India";
            String json = fetchUrl(apiUrl, "Microsoft Careers");
            if (json != null)
            {
                // operationResult.result.jobs
                int start = json.indexOf("\"jobs\":[");
                if (start != -1)
                {
                    start += 8;
                    int depth = 0, objStart = -1;
                    for (int i = start; i < json.length(); i++)
                    {
                        char c = json.charAt(i);
                        if (c == '{') { if (depth == 0) objStart = i; depth++; }
                        else if (c == '}')
                        {
                            depth--;
                            if (depth == 0 && objStart != -1)
                            {
                                String obj = json.substring(objStart, i + 1);
                                String title = extractString(obj, "\"title\":\"");
                                String jobUrl = extractString(obj, "\"jobPostingURL\":\"");
                                String loc = extractString(obj, "\"primaryLocation\":\"");

                                if (title != null && jobUrl != null && title.toLowerCase().contains("intern"))
                                {
                                    jobs.add(buildJob("Microsoft", title,
                                            jobUrl.replace("\\/", "/"),
                                            loc != null ? loc : "India", false, null,
                                            JobListing.ListingType.internship));
                                }
                                objStart = -1;
                            }
                        }
                        else if (c == ']' && depth == 0) break;
                    }
                }
            }
            System.out.println("[microsoft] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[microsoft] " + e.getMessage()); }
        return jobs;
    }

    // ─── Amazon ───────────────────────────────────────────────────────────────
    private List<JobListing> scrapeAmazon()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String url = "https://www.amazon.jobs/en/search.json?base_query=intern&loc_query=India&category%5B%5D=software-development&job_type%5B%5D=Full-Time,Part-Time,Temporary&result_limit=20&sort=relevant&offset=0";
            String json = fetchUrl(url, "Amazon Jobs");
            if (json == null) return jobs;

            int start = json.indexOf("\"jobs\":[");
            if (start == -1) return jobs;
            start += 8;

            int depth = 0, objStart = -1;
            for (int i = start; i < json.length(); i++)
            {
                char c = json.charAt(i);
                if (c == '{') { if (depth == 0) objStart = i; depth++; }
                else if (c == '}')
                {
                    depth--;
                    if (depth == 0 && objStart != -1)
                    {
                        String obj = json.substring(objStart, i + 1);
                        String title = extractString(obj, "\"title\":\"");
                        String jobPath = extractString(obj, "\"job_path\":\"");
                        String loc = extractString(obj, "\"city\":\"");
                        String desc = extractString(obj, "\"description_short\":\"");

                        if (title != null && jobPath != null && title.toLowerCase().contains("intern"))
                        {
                            jobs.add(buildJob("Amazon",
                                    title,
                                    "https://www.amazon.jobs" + jobPath,
                                    loc != null ? loc + ", India" : "India",
                                    false, desc, JobListing.ListingType.internship));
                        }
                        objStart = -1;
                    }
                }
                else if (c == ']' && depth == 0) break;
            }
            System.out.println("[amazon] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[amazon] " + e.getMessage()); }
        return jobs;
    }

    // ─── Flipkart ─────────────────────────────────────────────────────────────
    private List<JobListing> scrapeFlipkart()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Flipkart careers uses Lever ATS
            String url = "https://api.lever.co/v0/postings/flipkart?mode=json&team=Engineering";
            String json = fetchUrl(url, "Flipkart Lever");
            if (json != null) jobs.addAll(parseLeverJobs(json, "Flipkart", "Bangalore"));
            System.out.println("[flipkart] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[flipkart] " + e.getMessage()); }
        return jobs;
    }

    // ─── Swiggy ───────────────────────────────────────────────────────────────
    private List<JobListing> scrapeSwiggy()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Swiggy careers - scrape public listings page
            Document doc = fetchDoc("https://careers.swiggy.com/#/open-positions", "https://careers.swiggy.com/");
            if (doc == null)
            {
                // Fallback: try their Lever or Greenhouse ATS
                String json = fetchUrl("https://api.lever.co/v0/postings/swiggy?mode=json", "Swiggy Lever");
                if (json != null) jobs.addAll(parseLeverJobs(json, "Swiggy", "Bangalore"));
            }
            else
            {
                Elements cards = doc.select(".job-listing, .open-position, [class*=job-card]");
                for (Element card : cards)
                {
                    String title = card.select("h2, h3, .job-title, [class*=title]").text().trim();
                    String link = card.select("a").attr("href");
                    if (title.isEmpty()) continue;
                    String fullLink = link.startsWith("http") ? link : "https://careers.swiggy.com" + link;
                    jobs.add(buildJob("Swiggy", title, fullLink, "Bangalore", false, null, JobListing.ListingType.internship));
                }
            }
            System.out.println("[swiggy] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[swiggy] " + e.getMessage()); }
        return jobs;
    }

    // ─── Zomato ───────────────────────────────────────────────────────────────
    private List<JobListing> scrapeZomato()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Zomato uses Greenhouse ATS
            String json = fetchUrl("https://boards-api.greenhouse.io/v1/boards/zomato/jobs?content=true", "Zomato Greenhouse");
            if (json != null) jobs.addAll(parseGreenhouseJobs(json, "Zomato", "Gurugram"));
            System.out.println("[zomato] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[zomato] " + e.getMessage()); }
        return jobs;
    }

    // ─── Razorpay ─────────────────────────────────────────────────────────────
    private List<JobListing> scrapeRazorpay()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String json = fetchUrl("https://api.lever.co/v0/postings/razorpay?mode=json", "Razorpay Lever");
            if (json != null) jobs.addAll(parseLeverJobs(json, "Razorpay", "Bangalore"));
            System.out.println("[razorpay] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[razorpay] " + e.getMessage()); }
        return jobs;
    }

    // ─── CRED ─────────────────────────────────────────────────────────────────
    private List<JobListing> scrapeCRED()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String json = fetchUrl("https://boards-api.greenhouse.io/v1/boards/cred/jobs?content=true", "CRED Greenhouse");
            if (json != null) jobs.addAll(parseGreenhouseJobs(json, "CRED", "Bangalore"));
            System.out.println("[cred] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[cred] " + e.getMessage()); }
        return jobs;
    }

    // ─── Meesho ───────────────────────────────────────────────────────────────
    private List<JobListing> scrapeMeesho()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String json = fetchUrl("https://api.lever.co/v0/postings/meesho?mode=json", "Meesho Lever");
            if (json != null) jobs.addAll(parseLeverJobs(json, "Meesho", "Bangalore"));
            System.out.println("[meesho] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[meesho] " + e.getMessage()); }
        return jobs;
    }

    // ─── PhonePe ──────────────────────────────────────────────────────────────
    private List<JobListing> scrapePhonePe()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String json = fetchUrl("https://boards-api.greenhouse.io/v1/boards/phonepe/jobs?content=true", "PhonePe Greenhouse");
            if (json != null) jobs.addAll(parseGreenhouseJobs(json, "PhonePe", "Bangalore"));
            System.out.println("[phonepe] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[phonepe] " + e.getMessage()); }
        return jobs;
    }

    // ─── Ola ──────────────────────────────────────────────────────────────────
    private List<JobListing> scrapeOla()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Ola uses Lever
            String json = fetchUrl("https://api.lever.co/v0/postings/olacabs?mode=json", "Ola Lever");
            if (json != null) jobs.addAll(parseLeverJobs(json, "Ola", "Bangalore"));
            System.out.println("[ola] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[ola] " + e.getMessage()); }
        return jobs;
    }

    // ─── Zepto ────────────────────────────────────────────────────────────────
    private List<JobListing> scrapeZepto()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String json = fetchUrl("https://boards-api.greenhouse.io/v1/boards/zepto/jobs?content=true", "Zepto Greenhouse");
            if (json != null) jobs.addAll(parseGreenhouseJobs(json, "Zepto", "Mumbai"));
            System.out.println("[zepto] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[zepto] " + e.getMessage()); }
        return jobs;
    }

    // ─── BrowserStack ─────────────────────────────────────────────────────────
    private List<JobListing> scrapeBrowserStack()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String json = fetchUrl("https://boards-api.greenhouse.io/v1/boards/browserstack/jobs?content=true", "BrowserStack Greenhouse");
            if (json != null) jobs.addAll(parseGreenhouseJobs(json, "BrowserStack", "Mumbai"));
            System.out.println("[browserstack] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[browserstack] " + e.getMessage()); }
        return jobs;
    }

    // ─── Freshworks ───────────────────────────────────────────────────────────
    private List<JobListing> scrapeFreshworks()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            String json = fetchUrl("https://boards-api.greenhouse.io/v1/boards/freshworks/jobs?content=true", "Freshworks Greenhouse");
            if (json != null) jobs.addAll(parseGreenhouseJobs(json, "Freshworks", "Chennai"));
            System.out.println("[freshworks] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[freshworks] " + e.getMessage()); }
        return jobs;
    }

    // ─── Zoho ─────────────────────────────────────────────────────────────────
    private List<JobListing> scrapeZoho()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Zoho has a public careers page with HTML
            Document doc = fetchDoc("https://careers.zohocorp.com/jobs/Careers", "https://careers.zohocorp.com/");
            if (doc != null)
            {
                Elements listings = doc.select(".career-list li, .job-list-item, [class*=job-item]");
                for (Element el : listings)
                {
                    String title = el.select("a, h3, h4, .job-title").text().trim();
                    String link = el.select("a").attr("href");
                    if (title.isEmpty()) continue;
                    String fullLink = link.startsWith("http") ? link : "https://careers.zohocorp.com" + link;
                    jobs.add(buildJob("Zoho", title, fullLink, "Chennai", false, null,
                            title.toLowerCase().contains("intern")
                                    ? JobListing.ListingType.internship
                                    : JobListing.ListingType.full_time));
                }
            }
            System.out.println("[zoho] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[zoho] " + e.getMessage()); }
        return jobs;
    }

    // ─── Infosys ──────────────────────────────────────────────────────────────
    private List<JobListing> scrapeInfosys()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Infosys careers - scrape public page
            Document doc = fetchDoc("https://career.infosys.com/joblist", "https://career.infosys.com/");
            if (doc != null)
            {
                Elements listings = doc.select(".job-listing-row, .job-item, tr[class*=job], .career-listing");
                for (Element el : listings)
                {
                    String title = el.select("a, .job-title, td:first-child").text().trim();
                    String link = el.select("a").attr("href");
                    if (title.isEmpty() || title.length() < 4) continue;
                    String fullLink = link.startsWith("http") ? link : "https://career.infosys.com" + link;
                    jobs.add(buildJob("Infosys", title, fullLink, "India", false, null,
                            title.toLowerCase().contains("intern")
                                    ? JobListing.ListingType.internship
                                    : JobListing.ListingType.full_time));
                }
            }
            // Also check internship-specific page
            if (jobs.isEmpty())
            {
                jobs.add(buildJob("Infosys", "Internship Program (Check Website)",
                        "https://career.infosys.com/joblist", "India", false,
                        "Infosys offers internship programs. Visit the careers page for current openings.",
                        JobListing.ListingType.internship));
            }
            System.out.println("[infosys] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[infosys] " + e.getMessage()); }
        return jobs;
    }

    // ─── TCS ──────────────────────────────────────────────────────────────────
    private List<JobListing> scrapeTCS()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // TCS NextStep portal for freshers/interns
            Document doc = fetchDoc(
                "https://ibegin.tcs.com/iBegin/publiclistopportunity.do?source=Web_Career_Page&category=Intern",
                "https://ibegin.tcs.com/"
            );
            if (doc != null)
            {
                Elements listings = doc.select("tr.dataRow, .job-row, [class*=job-item]");
                for (Element el : listings)
                {
                    String title = el.select("a, td:nth-child(2), .jobtitle").text().trim();
                    String link = el.select("a").attr("href");
                    if (title.isEmpty()) continue;
                    String fullLink = link.startsWith("http") ? link : "https://ibegin.tcs.com" + link;
                    jobs.add(buildJob("TCS", title, fullLink, "India", false, null, JobListing.ListingType.internship));
                }
            }
            if (jobs.isEmpty())
            {
                jobs.add(buildJob("TCS", "Campus / Internship Program",
                        "https://www.tcs.com/careers/india/programs", "India", false,
                        "TCS offers NQT and internship programs. Check the website for dates.",
                        JobListing.ListingType.internship));
            }
            System.out.println("[tcs] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[tcs] " + e.getMessage()); }
        return jobs;
    }

    // ─── Wipro ────────────────────────────────────────────────────────────────
    private List<JobListing> scrapeWipro()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            Document doc = fetchDoc("https://careers.wipro.com/careers-home/jobs?location=India&type=Intern",
                    "https://careers.wipro.com/");
            if (doc != null)
            {
                Elements listings = doc.select(".job-tile, .job-item, article[class*=job]");
                for (Element el : listings)
                {
                    String title = el.select("h2, h3, .job-title, a").first() != null
                            ? el.select("h2, h3, .job-title, a").first().text().trim() : "";
                    String link = el.select("a").attr("href");
                    if (title.isEmpty()) continue;
                    String fullLink = link.startsWith("http") ? link : "https://careers.wipro.com" + link;
                    jobs.add(buildJob("Wipro", title, fullLink, "India", false, null,
                            title.toLowerCase().contains("intern")
                                    ? JobListing.ListingType.internship
                                    : JobListing.ListingType.full_time));
                }
            }
            if (jobs.isEmpty())
            {
                jobs.add(buildJob("Wipro", "Wipro Turbo / Internship Program",
                        "https://careers.wipro.com/", "India", false,
                        "Wipro offers Wipro Turbo and other intern programs. Check the careers page.",
                        JobListing.ListingType.internship));
            }
            System.out.println("[wipro] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[wipro] " + e.getMessage()); }
        return jobs;
    }

    // ─── Accenture ────────────────────────────────────────────────────────────
    private List<JobListing> scrapeAccenture()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            Document doc = fetchDoc(
                "https://www.accenture.com/in-en/careers/explore-careers/area-of-interest/technology-careers?jk=intern&cr=India",
                "https://www.accenture.com/"
            );
            if (doc != null)
            {
                Elements listings = doc.select(".job-listing-item, .cmp-teaser, [class*=job-card]");
                for (Element el : listings)
                {
                    String title = el.select("h3, h4, a[href*=job]").text().trim();
                    String link = el.select("a[href*=job]").attr("href");
                    if (title.isEmpty()) continue;
                    String fullLink = link.startsWith("http") ? link : "https://www.accenture.com" + link;
                    jobs.add(buildJob("Accenture", title, fullLink, "India", false, null,
                            title.toLowerCase().contains("intern")
                                    ? JobListing.ListingType.internship
                                    : JobListing.ListingType.full_time));
                }
            }
            if (jobs.isEmpty())
            {
                jobs.add(buildJob("Accenture", "Technology Graduate / Intern Program",
                        "https://www.accenture.com/in-en/careers", "India", false,
                        "Accenture offers campus and intern hiring programs in India.",
                        JobListing.ListingType.internship));
            }
            System.out.println("[accenture] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[accenture] " + e.getMessage()); }
        return jobs;
    }

    // ─── Paytm ────────────────────────────────────────────────────────────────
    private List<JobListing> scrapePaytm()
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            // Paytm uses Lever ATS
            String json = fetchUrl("https://api.lever.co/v0/postings/paytm?mode=json", "Paytm Lever");
            if (json != null) jobs.addAll(parseLeverJobs(json, "Paytm", "Noida"));
            System.out.println("[paytm] " + jobs.size() + " jobs");
        }
        catch (Exception e) { System.err.println("[paytm] " + e.getMessage()); }
        return jobs;
    }

    // ─── ATS Parsers ──────────────────────────────────────────────────────────

    /**
     * Parses Lever ATS JSON API response.
     * Format: [ { "text": title, "hostedUrl": url, "categories": { "location" } } ]
     */
    private List<JobListing> parseLeverJobs(String json, String companyName, String defaultLocation)
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
            if (!root.isArray()) return jobs;

            for (com.fasterxml.jackson.databind.JsonNode node : root)
            {
                String title = node.has("text") && !node.get("text").isNull() ? node.get("text").asText() : null;
                String hostedUrl = node.has("hostedUrl") && !node.get("hostedUrl").isNull() ? node.get("hostedUrl").asText() : null;
                String applyUrl = node.has("applyUrl") && !node.get("applyUrl").isNull() ? node.get("applyUrl").asText() : null;
                String url = hostedUrl != null ? hostedUrl : applyUrl;

                String loc = null;
                com.fasterxml.jackson.databind.JsonNode categories = node.get("categories");
                if (categories != null && categories.has("location") && !categories.get("location").isNull())
                {
                    loc = categories.get("location").asText();
                }

                if (title != null && url != null && !title.isEmpty() && !title.equalsIgnoreCase("What You Will Do")
                        && !title.equalsIgnoreCase("About the Role") && !title.equalsIgnoreCase("Responsibilities")
                        && !title.equalsIgnoreCase("Requirements") && title.length() > 5)
                {
                    String location = loc != null && !loc.isEmpty() ? loc : defaultLocation;
                    boolean isRemote = location.toLowerCase().contains("remote");

                    String desc = node.has("description") && !node.get("description").isNull() ? node.get("description").asText() : null;
                    if (desc != null && desc.length() > 2000) desc = desc.substring(0, 2000) + "...";

                    JobListing.ListingType type = title.toLowerCase().contains("intern")
                            ? JobListing.ListingType.internship
                            : JobListing.ListingType.full_time;

                    jobs.add(buildJob(companyName, title, url, location, isRemote, desc, type));
                }
            }
        }
        catch (Exception e) { System.err.println("[lever:" + companyName + "] " + e.getMessage()); }
        return jobs;
    }

    /**
     * Parses Greenhouse ATS JSON API response.
     * Format: { "jobs": [ { "title", "absolute_url", "location": { "name" }, "content" } ] }
     */
    private List<JobListing> parseGreenhouseJobs(String json, String companyName, String defaultLocation)
    {
        List<JobListing> jobs = new ArrayList<>();
        try
        {
            int jobsStart = json.indexOf("\"jobs\":[");
            if (jobsStart == -1) return jobs;
            jobsStart += 8;

            int depth = 0, objStart = -1;
            for (int i = jobsStart; i < json.length(); i++)
            {
                char c = json.charAt(i);
                if (c == '{') { if (depth == 0) objStart = i; depth++; }
                else if (c == '}')
                {
                    depth--;
                    if (depth == 0 && objStart != -1)
                    {
                        String obj = json.substring(objStart, i + 1);
                        String title = extractString(obj, "\"title\":\"");
                        String url = extractString(obj, "\"absolute_url\":\"");
                        String loc = extractString(obj, "\"name\":\""); // location.name
                        String desc = extractString(obj, "\"content\":\"");

                        if (title != null && url != null && !title.isEmpty())
                        {
                            String location = loc != null && !loc.isEmpty() ? loc : defaultLocation;
                            boolean isRemote = location.toLowerCase().contains("remote");
                            if (desc != null) { desc = desc.replaceAll("<[^>]+>", "").replace("\\n", "\n").trim(); }
                            if (desc != null && desc.length() > 2000) desc = desc.substring(0, 2000) + "...";

                            JobListing.ListingType type = title.toLowerCase().contains("intern")
                                    ? JobListing.ListingType.internship
                                    : JobListing.ListingType.full_time;
                            jobs.add(buildJob(companyName, title, url.replace("\\/", "/"),
                                    location, isRemote, desc, type));
                        }
                        objStart = -1;
                    }
                }
                else if (c == ']' && depth == 0) break;
            }
        }
        catch (Exception e) { System.err.println("[greenhouse:" + companyName + "] " + e.getMessage()); }
        return jobs;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private JobListing buildJob(String company, String title, String url,
                                 String location, boolean remote, String desc,
                                 JobListing.ListingType type)
    {
        JobListing job = new JobListing();
        job.setCompanyName(company.length() > 200 ? company.substring(0, 200) : company);
        job.setJobTitle(title.length() > 200 ? title.substring(0, 200) : title);
        job.setSourceUrl(url.length() > 500 ? url.substring(0, 500) : url);
        job.setSource(source);
        job.setLocation(location != null && location.length() > 200 ? location.substring(0, 200) : location);
        job.setIsRemote(remote || (location != null && location.toLowerCase().contains("remote")));
        job.setDescription(desc != null ? org.jsoup.Jsoup.parse(desc).text() : null);
        job.setListingType(type);
        job.setStatus(JobListing.Status.ACTIVE);
        return job;
    }

    private String fetchUrl(String url, String label)
    {
        try
        {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json,text/html")
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) { System.err.println("[" + label + "] HTTP " + res.statusCode()); return null; }
            return res.body();
        }
        catch (Exception e) { System.err.println("[" + label + "] " + e.getMessage()); return null; }
    }

    private Document fetchDoc(String url, String referer)
    {
        try
        {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", referer)
                    .timeout(20000)
                    .get();
        }
        catch (Exception e) { System.err.println("[" + url + "] " + e.getMessage()); return null; }
    }

    private String extractString(String json, String key)
    {
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if (escaped) { if (c == 'n') sb.append('\n'); else if (c == 't') sb.append('\t'); else if (c == 'r') sb.append('\r'); else sb.append(c); escaped = false; }
            else if (c == '\\') escaped = true;
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }
}
