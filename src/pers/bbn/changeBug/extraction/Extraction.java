package pers.bbn.changeBug.extraction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 提取数据的超类。
 * <p>
 * 将commit按照时间排序，提取指定范围内所有commit的若干信息。若干信息的提取分别
 * 由三个子类去实现。需要注意的是，由于miningit分配给各commit的id并不是其实际提交的
 * 顺序（由于多线程并发导致），所以对于commit的排序不应根据其id排序，而应根据 commit_date排序。
 * 
 * @param sql
 *            需要连接的数据库
 * @param start
 *            指定的commit的起始值（按照时间排序）
 * @param end
 *            指定的commit的结束值（按照时间排序）
 * @see ResultSet
 * @see SQLException
 * @see Statement
 * @author niu
 *
 */
public abstract class Extraction {
    String sql;
    Statement stmt;
	ResultSet resultSet;
	int start;
	int end;
	List<List<Integer>> commit_fileIds;
	List<Integer> commit_ids;
	List<Integer> commitIdPart;
	SQLConnection sqlL;
	String databaseName;

	/**
	 * extraction2提取信息并不需要miningit生成的数据，此构造函数只是为了统一接口。
	 * 
	 * @param database
	 * @throws Exception 
	 */
	public Extraction(String database,int start,int end) throws Exception  { // 为extraction2提供构造函数。	
		this.start=start;
		this.end=end;
		this.databaseName=database;
		this.sqlL = new SQLConnection(database);
		this.stmt = sqlL.getStmt();
		initialCommitFileIds();
	}

	/**
	 * 根据给定的范围获取有效的commit_file对,也就是包括删除在内的记录对.
	 * @throws Exception
	 */
	private void initialCommitFileIds() throws Exception {
		commit_ids=new ArrayList<>();
		commitIdPart=new ArrayList<>();
		sql = "select id from scmlog order by commit_date";
		resultSet = stmt.executeQuery(sql);
		while (resultSet.next()) {
			commit_ids.add(resultSet.getInt(1));
		}
		if (start<0) {
			throw new Exception("start can't be less 0!");
		}
		if (end>commit_ids.size()) {
			throw new Exception("end is larger than the total number of commits!");
		}
		for (int i = start-1; i < end; i++) {
			sql="select file_id,current_file_path from actions where commit_id="+commit_ids.get(i)+" and type!='D'";
			resultSet=stmt.executeQuery(sql);
			while (resultSet.next()) {
				String current_file_path=resultSet.getString(2);
				if (current_file_path.endsWith(".java")&&(!current_file_path.toLowerCase().contains("test"))) {
					List<Integer> tmp=new ArrayList<>();
					tmp.add(commit_ids.get(i));
					tmp.add(resultSet.getInt(1));
					commit_fileIds.add(tmp);
					commitIdPart.add(resultSet.getInt(commit_ids.get(i)));
				}
			}
		}
	}

	/**
	 * 按时间序返回有效的commit_fileId列表。
	 * 
	 * @return 按时间排序的指定范围内的commit_id列表。
	 */
	public List<List<Integer>> getCommit_FileIds() {
		return commit_fileIds;
	}

	/**
	 * 根据给定的一组commit_fileId对,获取其对应的内容.
	 * 需要注意的是stringBuffer格式的content后面以逗号结尾
	 * @param commit_fileIds
	 * @return
	 * @throws SQLException
	 */
	public abstract Map<List<Integer>, StringBuffer> getContentMap(List<List<Integer>> someCommit_fileIds) throws SQLException;

}
