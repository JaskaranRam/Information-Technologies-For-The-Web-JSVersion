package controllers;

import DAO.ArticleDAO;
import DAO.AuctionDAO;
import DAO.OfferDAO;
import DAO.UserDAO;
import beans.Offer;
import beans.User;
import utils.AuctionDetailsInfo;
import utils.ConnectionHandler;
import utils.Pair;

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
import java.util.LinkedHashMap;
import java.util.List;

@WebServlet("/GoToAuctionDetails")
public class GoToAuctionDetails extends HttpServlet {
	@Serial
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	private OfferDAO offerDAO;
	private AuctionDAO auctionDAO;
	private ArticleDAO articleDAO;
	private UserDAO userDAO;
	
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionHandler.getConnection(servletContext);

		offerDAO = new OfferDAO(connection);
		auctionDAO = new AuctionDAO(connection);
		articleDAO = new ArticleDAO(connection);
		userDAO = new UserDAO(connection);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		int auctionId;
		AuctionDetailsInfo auctionDetailsInfo = null;


		try{
			auctionId = Integer.parseInt(request.getParameter("auctionId"));
		} catch(NumberFormatException e){
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("auctionId must be an integer value");
			return;
		}

		try {

			List<Pair<Offer, String>> auctionOffers;

			LinkedHashMap<Integer, String> users = new LinkedHashMap<>();

			User awardedUser = null;

			try {
				auctionDetailsInfo = auctionDAO.getAuctionDetails(auctionId);
				if (auctionDetailsInfo == null) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().println("auctionId is not valid");
					return;
				}
			} catch (SQLException e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Unable to access the database");
				return;
			}

			try {
				// WINNING OFFERS IS AT TOP, IF EXISTS
				auctionOffers = offerDAO.getOffers(auctionId);

				if(!auctionOffers.isEmpty()){
					awardedUser = userDAO.getUser(auctionOffers.get(0).getFirst().getUser());
					if(awardedUser != null){
						// Removes the password from the object for security purposes
						awardedUser.setPassword("");
					}
					auctionDetailsInfo.addOfferWinner(auctionOffers,awardedUser);
				}

			}catch(SQLException e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Error accessing the database!");
				return;
			}


			// Redirect to AuctionDetails
			String path = "/WEB-INF/" + page;
			ServletContext servletContext = getServletContext();
			//final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			//ctx.setVariable("auctionId", auctionId);
			//ctx.setVariable("auction", auction);
			//ctx.setVariable("article", articles);
			//ctx.setVariable("frmtDeadline", frmtDeadline);
			//ctx.setVariable("isExpired", isExpired);
			//ctx.setVariable("offers", frmtAuctionOffers);
			//ctx.setVariable("users", users);
			//ctx.setVariable("maxAuctionOffer", maxAuctionOffer);
			//ctx.setVariable("awardedUser", awardedUser);
			//ctx.setVariable("imageMap", imageMap);
			//templateEngine.process(path, ctx, response.getWriter());
		} catch (SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error processing the servlet!");
		}
	}
	

	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
