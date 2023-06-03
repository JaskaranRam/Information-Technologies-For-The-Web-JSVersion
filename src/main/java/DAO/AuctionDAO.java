package DAO;

import beans.Article;
import beans.Auction;
import utils.AuctionDetailsInfo;
import utils.AuctionFullInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class AuctionDAO {
	private Connection connection;
	PreparedStatement pstatement = null;
	ResultSet result = null;
	
	public AuctionDAO(Connection conn) {
		this.connection = conn;
	}
	
	public int insertAuction(LocalDateTime expiring_date, int minimum_raise, int creator) throws SQLException{
		int auction_id = -1;
		try {
			pstatement = connection.prepareStatement("INSERT INTO auction (expiring_date, minimum_raise, creator) VALUES(?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			pstatement.setObject(1, expiring_date);
			pstatement.setInt(2, minimum_raise);
			pstatement.setInt(3, creator);
			int result = pstatement.executeUpdate();
			if(result > 0) {
				//System.out.println("Auction inserted successfully");
				ResultSet resultSet = pstatement.getGeneratedKeys();
				if(resultSet.next()) {
					auction_id = resultSet.getInt(1);
					//System.out.println("Auction id: " + auction_id); relativo all'asta appena aggiunta
				}
				resultSet.close();
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				pstatement.close();
			} catch(Exception e2) {
				throw new SQLException(e2);
			}
		}
		return auction_id;
	}

	public List<Auction> search(String keyword, LocalDateTime time) throws SQLException{
		List<Auction> filteredAuctions = new ArrayList<>();
		try{
			pstatement = connection.prepareStatement("SELECT DISTINCT au.* FROM auction au JOIN article ar ON ar.auction_id = au.auction_id WHERE (ar.name LIKE ? OR ar.description LIKE ?) AND au.expiring_date > ? AND au.open = '1' ORDER BY au.expiring_date ASC");
			pstatement.setString(1, "%" + keyword.toUpperCase() + "%");
			pstatement.setString(2, "%" + keyword.toUpperCase() + "%");
			pstatement.setObject(3, time);
			result = pstatement.executeQuery();
			while (result.next()) {
				filteredAuctions.add(resultToAuction(result));
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				result.close();
			} catch(Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch(Exception e2) {
				throw new SQLException(e2);
			}
		}
		return filteredAuctions;
	}

	public List<AuctionFullInfo> getFiltered(String keyword, LocalDateTime time) throws SQLException{

		List<AuctionFullInfo> auctionFullList = new ArrayList<>();

		try{
			pstatement = connection.prepareStatement("SELECT * FROM auction x JOIN article y on x.auction_id= y.auction_id where x.auction_id = (SELECT DISTINCT au.auction_id FROM auction au JOIN article ar ON ar.auction_id = au.auction_id WHERE (ar.name LIKE ? OR ar.description LIKE ?) AND au.expiring_date > ? AND au.open = '1') ORDER BY x.expiring_date ASC, y.article_id ASC");
			pstatement.setString(1, "%" + keyword.toUpperCase() + "%");
			pstatement.setString(2, "%" + keyword.toUpperCase() + "%");
			pstatement.setObject(3, time);
			result = pstatement.executeQuery();
			int prev_auction_id = -1;
			while (result.next()) {
				Auction auction = resultToAuction(result);
				Article article = resultToArticle(result);
				if(auction.getAuction_id() == prev_auction_id){
					auctionFullList.get(auctionFullList.size()-1).addArticle(article);
				}
				else{
					List<Article> articles = new ArrayList<>();
					articles.add(article);
					auctionFullList.add(new AuctionFullInfo(auction, articles, null));
					prev_auction_id = auction.getAuction_id();
				}
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				result.close();
			} catch(Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch(Exception e2) {
				throw new SQLException(e2);
			}
		}
		return auctionFullList;
	}

	public AuctionDetailsInfo getAuctionDetails (int auction_id) throws SQLException {
		AuctionDetailsInfo elem;

		Auction auction = null;
		List<Article> articles = new ArrayList<>();
		boolean firstTime = true;
		try {
			pstatement = connection.prepareStatement("SELECT * FROM auction x JOIN article y on x.auction_id = y.auction_id WHERE x.auction_id = ?");
			pstatement.setInt(1, auction_id);
			result = pstatement.executeQuery();
			while(result.next()) {
				if (firstTime){
					 auction = resultToAuction(result);
					firstTime = false;
				}
				Article article = resultToArticle(result);
				articles.add(article);
			}
			elem = new AuctionDetailsInfo(auction, articles, null, null);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}
		return elem;
	}

	public List<Auction> getOpenAuctions(int user_id) throws SQLException{
		List<Auction> auctions = new ArrayList<>();	
		try {
			pstatement = connection.prepareStatement("SELECT * FROM auction WHERE creator = ? AND open = '1'");
			pstatement.setInt(1, user_id);
			result = pstatement.executeQuery();
			while (result.next()) {
				auctions.add(resultToAuction(result));
			}
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);

		} finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}	
		return auctions;
	}
	
	public List<Auction> getClosedAuctions(int user_id) throws SQLException{
		List<Auction> auctions = new ArrayList<>();	
		try {
			pstatement = connection.prepareStatement("SELECT * FROM auction WHERE creator = ? AND open = '0'");
			pstatement.setInt(1, user_id);
			result = pstatement.executeQuery();
			while(result.next()) {
				auctions.add(resultToAuction(result));
			}
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);

		} finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}	
		return auctions;
	}

	public LinkedHashMap<Auction, List<Article>> getAuctionsByUser(int user_id) throws SQLException{
		LinkedHashMap<Auction, List<Article>> userAuctions = new LinkedHashMap<>();
		try {
			pstatement = connection.prepareStatement("SELECT * FROM auction x JOIN article y on x.auction_id = y.auction_id WHERE creator = ?");
			pstatement.setInt(1, user_id);
			result = pstatement.executeQuery();
			while(result.next()) {
				Auction auction = resultToAuction(result);
				Article article = resultToArticle(result);
				if(userAuctions.containsKey(auction)){
					userAuctions.get(auction).add(article);
				}
				else{
					List<Article> articles = new ArrayList<>();
					articles.add(article);
					userAuctions.put(auction, articles);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);

		} finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}
		return userAuctions;
	}
	
	public Auction getAuction(int auction_id) throws SQLException{
		Auction auction = null;
		try {
			pstatement = connection.prepareStatement("SELECT * FROM auction WHERE auction_id = ?");
			pstatement.setInt(1, auction_id);
			result = pstatement.executeQuery();
			if(result.next())
				auction = resultToAuction(result);
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);

		} finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}	
		return auction;
	}

	
	public boolean changeAuctionStatus(int auction_id) throws SQLException{
		int outcome = 0;
		try {
			pstatement = connection.prepareStatement("UPDATE auction SET open = 0 WHERE auction_id = ?");

			// AGGIUNGERE ARTCLE POSTO A SOLD = 1

			pstatement.setInt(1, auction_id);
			pstatement.executeUpdate();
			//FAI CONTROLLO
			// If there is an affected row, it means that the auction has been closed
			if(outcome > 0)
				return true;
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				pstatement.close();
			} catch (Exception e1) {}
		}		
		return false;
	}

	public void setInitialPrice(int auction_id, int initialPrice) throws SQLException{
		int outcome = -1;
		try {
			pstatement = connection.prepareStatement("UPDATE auction SET initial_price = ? WHERE auction_id = ?");
			pstatement.setInt(1, initialPrice);
			pstatement.setInt(2, auction_id);
			outcome = pstatement.executeUpdate();
			if (outcome == 0)
				throw new SQLException("No auction with id " + auction_id + " found");

		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				pstatement.close();
			} catch (Exception e1) {}
		}
	}

	public boolean checkUserId(int auction_id, int user_id) throws SQLException {
		int outcome = -1;
		try {
			pstatement = connection.prepareStatement("SELECT auction_id, creator FROM auction WHERE auction_id = ?");
			pstatement.setInt(1, auction_id);
			result = pstatement.executeQuery();
			if(result.next()) {
				if(result.getInt("creator") == user_id)
					return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);
		}  finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}
		return false;
	}
	
	private Auction resultToAuction(ResultSet result) throws SQLException{
		Auction auction = new Auction();
		auction.setAuction_id(result.getInt("auction_id"));
		auction.setOpen(result.getBoolean("open"));
		auction.setCreator(result.getInt("creator"));
		auction.setInitial_price(result.getInt("initial_price"));
		auction.setMinimum_raise(result.getInt("minimum_raise"));
		auction.setExpiring_date(result.getTimestamp("expiring_date").toLocalDateTime());
		return auction;
	}

	public Article resultToArticle(ResultSet result) throws SQLException{
		Article article = new Article();
		article.setArticle_id(result.getInt("article_id"));
		article.setName(result.getString("name"));
		article.setDescription(result.getString("description"));
		article.setImage(result.getBlob("image"));
		article.setArticle_creator(result.getInt("article_creator"));
		article.setAuction_id(result.getInt("auction_id"));
		article.setPrice(result.getInt("price"));
		article.setSold(result.getBoolean("sold"));

		return article;
	}

}
