package controllers;

import DAO.ArticleDAO;
import DAO.AuctionDAO;
import DAO.OfferDAO;
import beans.Article;
import beans.Auction;
import beans.Offer;
import beans.User;
import utils.ConnectionHandler;
import utils.DiffTime;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/WonOffers")
public class WonOffers extends HttpServlet {
	@Serial
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	AuctionDAO auctionDAO;
	ArticleDAO articleDAO;
	OfferDAO offerDAO;

	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionHandler.getConnection(getServletContext());

		auctionDAO = new AuctionDAO(connection);
		articleDAO = new ArticleDAO(connection);
		offerDAO = new OfferDAO(connection);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User user;
		Map<Integer, List<Article>> awardedAuctions = new HashMap<>();
		Map<Integer, Offer> winningOffers;
		List<Auction> filteredAuctions = null;
		Map<Integer, List<Article>> map = new HashMap<>();
		HashMap<Integer, DiffTime> remainingTimes = new HashMap<Integer, DiffTime>();
		LocalDateTime logLdt = null;
		LocalDateTime currLdt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

		LinkedHashMap<Auction,List<Article>> orderedFilteredMap= new LinkedHashMap<>();

		try{
			user = (User) request.getSession().getAttribute("user");
			logLdt = ((LocalDateTime) request.getSession(false).getAttribute("creationTime")).truncatedTo(ChronoUnit.MINUTES);
		} catch (NullPointerException e){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error, user not logged in correctly!");
			return;
		}

		try{
			winningOffers = offerDAO.getWinningOfferByUser(user.getUser_id());
			for(Integer auction : winningOffers.keySet()){
				awardedAuctions.put(auction, articleDAO.getAuctionArticles(auction));
			}
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover the winning offers");
			return;
		}

		String key = request.getParameter("key");
		if (key != null){
			if(!validateKey(key)){
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not valid key!, key must contain only letters and be longer than 2 characters, but less than 63");
				return;
			}
			try {
				filteredAuctions = auctionDAO.search(key, logLdt);
			} catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error accessing the database!");
				return;
			}
		}

		if(filteredAuctions != null){
			for(Auction auction : filteredAuctions){
				try {
					map.put(auction.getAuction_id(), articleDAO.getAuctionArticles(auction.getAuction_id()));
					remainingTimes.put(auction.getAuction_id(), DiffTime.getRemainingTime(currLdt, auction.getExpiring_date()));
				} catch (SQLException e) {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error accessing the database!");
					return;
				}
			}
		}

		String path = "/WEB-INF/purchase.html";
		ServletContext servletContext = getServletContext();
		//final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		//ctx.setVariable("key", key);
		//ctx.setVariable("filteredAuctions", filteredAuctions);
		//ctx.setVariable("awardedAuctions", awardedAuctions);
		//ctx.setVariable("winningOffers", winningOffers);
		//ctx.setVariable("map", map);
		//ctx.setVariable("remainingTimes", remainingTimes);
		//templateEngine.process(path, ctx, response.getWriter());
	}

	private boolean validateKey(String key){
    	// Checks if the key contains only letters and is longer than 2 characters, but less than 63
		return key.matches("[a-zA-Z]+") && key.length() > 2 && key.length() < 63;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}

