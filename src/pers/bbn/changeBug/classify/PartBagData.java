package pers.bbn.changeBug.classify;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pers.bbn.changeBug.extraction.Extraction1;
import pers.bbn.changeBug.extraction.Extraction2;
import pers.bbn.changeBug.extraction.Extraction3;
import weka.core.Instances;

/**
 * 为PartBagging组合数据.
 * @author niu
 *
 */
public class PartBagData {

	private List<List<Integer>> commitFileIds;
	private Map<List<Integer>, String> classLabels;
	private static final String bufferDir = "/home/niu/bufferDir";
	private List<Instances> datas;
	private String database;
	private List<String> filesName = Arrays.asList("MetaData.csv",
			"Metric.csv", "Bow.csv");
	
	public PartBagData(String database, int start, int end, String metricsFile,
			String projectHome) throws Exception{
		this.database = database;
		Extraction1 extraction1 = new Extraction1(database, start, end);
		commitFileIds = extraction1.getCommit_file_inExtracion1();
		classLabels = extraction1.getClassLabels(commitFileIds);
		Map<List<Integer>, StringBuffer> metaDataContent = extraction1
				.getContentMap(commitFileIds);

		Extraction2 extraction2 = new Extraction2(database, start, end);
		extraction2.extraFromTxt(metricsFile);
		Map<List<Integer>, StringBuffer> matricsContent = extraction2
				.getContentMap(commitFileIds);

		Extraction3 extraction3 = new Extraction3(database, projectHome, start,
				end,false);
		Map<List<Integer>, StringBuffer> bowContent = extraction3
				.getContentMap(commitFileIds);
		List<Map<List<Integer>, StringBuffer>> contentsMap = Arrays.asList(
				metaDataContent, matricsContent, bowContent);

		try {
			createPartCsv(database, filesName, contentsMap, classLabels);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		//是否先对bow做属性选择覆盖原有CSV文件?
		//
		//
		System.out.println("不同部分实例集写入文件成功!");
	}
	
	/**
	 * 取该数据库对应工程的不同部分的实例集.
	 * @throws IOException
	 */
	public void getInstanceList()  {
		for (String fileName : filesName) {
			fileName = database+fileName;
			Instances data;
			try {
				data = PreProcess.readInstancesFromCSV(bufferDir + "/"
						+ fileName);
				datas.add(data);
			} catch (IOException e) {
				System.out.println("从CSV文件获取实例失败!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 根据数据库名(也就是工程名),不同部分的属性内容,类标签的对应关系,创建不同部分的csv文件,用于分别训练基分类器.
	 * 
	 * @param database
	 *            数据库名,也是工程名
	 * @param filesName
	 *            得到的一系列csv文件的名称
	 * @param contentsMap
	 *            包含不同部分属性的Map
	 * @param classLabels
	 *            包含类标签属性的Map
	 * @throws Exception
	 */
	private void createPartCsv(String database, List<String> filesName,
			List<Map<List<Integer>, StringBuffer>> contentsMap,
			Map<List<Integer>, String> classLabels) throws Exception {
		if (filesName == null || contentsMap == null
				|| filesName.size() != contentsMap.size()) {
			throw new Exception("can't determine correspondence");
		}
		for (int i = 0; i < filesName.size(); i++) {
			try {
				writeContentMapToCsvFile(database + filesName.get(i),
						contentsMap.get(i), classLabels);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 根据给定的content,以及类标签的对应关系,将实例信息写入csv文件,以便读取instance实例.
	 * 
	 * @param fileName
	 *            写入的文件的名称,命名规则为 工程名+MateData/Metrics/Bow.csv
	 * @param content
	 *            包含实例属性信息的Map,不包含类标签.
	 * @param labelsMap
	 *            包含类标签信息的Map.
	 * @throws Exception
	 */
	public void writeContentMapToCsvFile(String fileName,
			Map<List<Integer>, StringBuffer> content,
			Map<List<Integer>, String> labelsMap) throws Exception {
		File dirFile = new File(bufferDir);
		if (!dirFile.exists()) {
			dirFile.mkdir();
		}
		File csvFile = new File(bufferDir + "/" + fileName);
		if (!csvFile.exists()) {
			try {
				if (csvFile.createNewFile()) {
					System.out.println(csvFile + " create succeed!");
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(csvFile + " create fail!");
				throw e;
			}
		}
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
				csvFile));
		List<Integer> titleKey = new ArrayList<>();
		titleKey.add(-1);
		titleKey.add(-1);
		bufferedWriter.append(content.get(titleKey) + "bug_introducing" + "\n");
		for (List<Integer> list : commitFileIds) {
			if (!content.containsKey(list)) {
				bufferedWriter.close();
				throw new Exception("content don't have the of <" + list.get(0)
						+ "," + list.get(1) + ">");
			}
			if (!labelsMap.containsKey(list)) {
				bufferedWriter.close();
				throw new Exception("labels don't have the of <" + list.get(0)
						+ "," + list.get(1) + ">");
			}
			if (labelsMap.get(list).equals("1")) {
				bufferedWriter.append(content.get(list) + "True" + "\n");
			} else {
				bufferedWriter.append(content.get(list) + "False" + "\n");
			}
		}
		bufferedWriter.flush();
		bufferedWriter.close();
	}
}
