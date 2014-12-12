package presentation;

import entities.User;
import presentation.util.JsfUtil;
import presentation.util.PaginationHelper;
import boundary.UserFacade;
import common.UserType;
import java.io.IOException;

import java.io.Serializable;
import java.util.Date;
import java.util.ResourceBundle;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpSession;

@ManagedBean(name = "userController")

@RequestScoped
public class UserController implements Serializable {

    private User current;
    private DataModel items = null;
    @EJB
    private boundary.UserFacade ejbFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;

    private String loginEmail;
    private String loginPassword;

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }

    public UserController() {

    }

    public String distrotyUserSession() {
        //FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("logged_in_user");

        return "User session destroyed";
        //logged_in_user
    }
    public void logout() throws IOException{
        if (isLoggedIn()){
             FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        }
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.redirect(ec.getRequestContextPath() + "/index.xhtml");
    }
    public boolean isLoggedIn(){
        
        return ((User)loggedInUser()==null)?false:true;
    }
    public User loggedInUser() {
        FacesContext cur=FacesContext.getCurrentInstance();
        return (User) cur.getExternalContext().getSessionMap().get("logged_in_user");
    }

    public int isUserLoggedIn() throws IOException {
        FacesContext cur=FacesContext.getCurrentInstance();
        User u = (User) cur.getExternalContext().getSessionMap().get("logged_in_user");

        if (u != null) {
            return 1;
        } else {

            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            context.redirect(context.getRequestContextPath() + "/faces/user/login.xhtml");

      //  FacesContext.getCurrentInstance().getExternalContext().redirect("../user/login.xhtml");
            return 0;
        }
    }

    public String handleLogin() throws IOException {

        //System.err.println("output");
        String loginQuery = "SELECT s FROM User s WHERE s.email=:email AND s.password=:password";

       // System.out.println(loginQuery);
        TypedQuery<User> query = getFacade().getEM().createQuery(loginQuery, User.class);
        query.setParameter("email", getLoginEmail());
        query.setParameter("password", getLoginPassword());

        try {
            User usr = query.getResultList().get(0);
            //System.out.println(usr.getEmail());
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("logged_in_user", usr);

            usr.setLastLoginDate(new Date());

            getFacade().edit(usr);

          //  getFacade().getEM().merge(current);
            FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
            return "dashboard";

        } catch (Exception e) {
            // e.printStackTrace();
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid Login", "Invalid Login: Please check your username of password"));
            FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml");

            return "login";
        }

    }

    public String getLoginEmail() {
        return loginEmail;
    }

    public void setLoginEmail(String loginEmail) {
        this.loginEmail = loginEmail;
    }

    public User getCurrent() {
        return current;
    }

    public void setCurrent(User current) {
        this.current = current;
    }

    public User getSelected() {
        if (current == null) {
            current = new User();
            selectedItemIndex = -1;
        }
        return current;
    }

    private UserFacade getFacade() {
        return ejbFacade;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(10) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        current = (User) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new User();
        selectedItemIndex = -1;
        return "dashboard";
    }

    public String create() {
        try {
            current.setLastLoginDate(new Date());
            current.setRegisterDate(new Date());
            current.setType(UserType.Visitor);
            getFacade().create(current);

            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("logged_in_user", current);

            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Resource/Bundle").getString("UserCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Resource/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit() {
        current = (User) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Resource/Bundle").getString("UserUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Resource/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (User) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreatePagination();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Resource/Bundle").getString("UserDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Resource/Bundle").getString("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    private void recreateModel() {
        items = null;
    }

    private void recreatePagination() {
        pagination = null;
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public User getUser(java.lang.Long id) {
        return ejbFacade.find(id);
    }

    @FacesConverter(forClass = User.class)
    public static class UserControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserController controller = (UserController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userController");
            return controller.getUser(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof User) {
                User o = (User) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + User.class.getName());
            }
        }

    }

}
