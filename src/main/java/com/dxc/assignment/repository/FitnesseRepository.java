package com.dxc.assignment.repository;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Repository;

import com.dxc.assignment.model.AreaChart;
import com.dxc.assignment.model.PieChart;

@Repository
@PropertySource("classpath:setting.properties")
public class FitnesseRepository {
	@Value("${spring.fitness.connect.url}")
	private String urlFitnesse;

	public String getUrlFitnesse() {
		return urlFitnesse;
	}

	public void setUrlFitnesse(String urlFitnesse) {
		this.urlFitnesse = urlFitnesse;
	}

	public PieChart getPieChart(String project) {
		List<AreaChart> areas = getAreaChart(project);
		PieChart p = new PieChart();
		int sumFailed = areas.stream().mapToInt(AreaChart::getFailed).sum();
		int sumPassed = areas.stream().mapToInt(AreaChart::getPassed).sum();
		p.setFailed(sumFailed);
		p.setPassed(sumPassed);
		return p;
	}
	/**
	 * This method create list areachart model of prject
	 * @param project
	 * @return
	 */
	public List<AreaChart> getAreaChart(String project) {
		String path = urlFitnesse + "/FrontPage." + project;
		List<AreaChart> areas = new ArrayList<>();
		List<String> testSuites = getTestSuites(path);
		for (String testSuite : testSuites) {
			String pageHistory = path + "." + testSuite + "?pageHistory";
			Document doc;
			try {
				doc = Jsoup.connect(pageHistory).get();
				Elements trs = doc.select("table tr[id]");
				for (Element tr : trs) {
					String id = tr.attr("id");
					String date = id.substring(id.indexOf("_")+1, id.length()-6);
					int passed = 0, failed = 0;
					Elements tds = tr.select("td");
					passed = Integer.parseInt(tds.select("[class^=pass_count").text());
					failed = Integer.parseInt(tds.select("[class^=fail_count").text());
					AreaChart area = new AreaChart();
					area.setDate(date);
					area.setFailed(failed);
					area.setPassed(passed);
					areas.add(area);
				}
				
			} catch (Exception e) {
			}
		}
		return uniqueAreaChartWithDate(areas);
	}
	/**
	 * Create list with unique value date
	 * @param list
	 * @return
	 */
	public List<AreaChart> uniqueAreaChartWithDate(List<AreaChart> list) {
		Map<String, AreaChart> map = new HashMap<>();
		
		for (AreaChart areaChart : list) {
			String date = areaChart.getDate();
			AreaChart sum = map.get(date);
			if (sum == null) {
				sum = new AreaChart();
				sum.setDate(date);
				sum.setFailed(0);
				sum.setPassed(0);
				map.put(date, sum);
			}
			sum.setFailed(sum.getFailed() + areaChart.getFailed());
			sum.setPassed(sum.getPassed() + areaChart.getPassed());
		}
		List<AreaChart> result = new ArrayList<AreaChart>(map.values());
		ComparatorWithDate comparator = new ComparatorWithDate();
		Collections.sort(result, comparator);
		String maxDate = buildDateInFourWeek(result.get(result.size()-1).getDate());
		
		return removeOutOfFourWeek(maxDate, result);
	}
	

	/**
	 * Get all testsuite in project 
	 * @param path example http://localhost:8083/FrontPage.SuitProject001
	 * @return
	 */
	public List<String> getTestSuites(String path) {
		List<String> testSuites = new ArrayList<String>();
		try {
			Document doc = Jsoup.connect(path).get();
			Elements table = doc.select("table");
			Elements td = table.select("td a");
			for (Element element : td) {
				String href = element.attr("href");
				String name = href.substring(href.lastIndexOf(".") + 1);
				testSuites.add(name);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return testSuites;
	}
	
	public static String buildDateInFourWeek(String date) {
		Calendar recentDate = Calendar.getInstance();
		int year1 = Integer.parseInt(date.substring(0, 4));
		int month1 = Integer.parseInt(date.substring(4, 6));
		int day1 = Integer.parseInt(date.substring(6, 8));
		recentDate.set(year1, month1-1, day1);
		recentDate.add(Calendar.WEEK_OF_YEAR, -4);
		Date res = recentDate.getTime();
		String format = new SimpleDateFormat("yyyyMMdd").format(res);
		return format;
	}

	public List<AreaChart> removeOutOfFourWeek(String date, List<AreaChart> list) {
		int index = 0;
		ComparatorWithDate comparator = new ComparatorWithDate();
		AreaChart area = new AreaChart();
		area.setDate(date);
		if (list.contains(area)) {
			 index = list.indexOf(area);
			 for (int i = list.size(); i >= 0; i--) {
					if (i < index) {
						list.remove(i);
					}
				}
		} else {
			list.add(area);
			Collections.sort(list, comparator);
			index = list.indexOf(area);
			for (int i = list.size(); i >= 0; i--) {
				if (i <= index) {
					list.remove(i);
				}
			}
		}
		
		return list;
	}

}