/*
 * GenericHibernateDAO.java
 *
 * Created on June 28, 2007, 7:14 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.persistence;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;

/**
 *
 * @author harri
 */
public abstract class GenericHibernateDAO<T, ID extends Serializable>
        implements GenericDAO<T, ID> {
    
    private Class<T> persistentClass;
    
    private Session session;
    
    public GenericHibernateDAO() {
        this.persistentClass = (Class<T>)
        ( (ParameterizedType) getClass().getGenericSuperclass() )
        .getActualTypeArguments()[0];
    }
    
    public void setSession(Session s) {
        this.session = s;
    }
    
    protected Session getSession() {
        if (session == null)
            session = HibernateUtil.getSessionFactory()
            .getCurrentSession();
        return session;
    }
    
    public Class<T> getPersistentClass() {
        return persistentClass;
    }
    
    @SuppressWarnings("unchecked")
    public T findById(ID id, boolean lock) {
        T entity;
        if (lock)
            entity = (T) getSession()
            .load(getPersistentClass(), id, LockMode.UPGRADE);
        else
            entity = (T) getSession()
            .load(getPersistentClass(), id);
        return entity;
    }
    
    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        return findByCriteria();
    }
    
    @SuppressWarnings("unchecked")
    public List<T> findByExample(T exampleInstance,
            String... excludeProperty) {
        Criteria crit =
                getSession().createCriteria(getPersistentClass());
        Example example = Example.create(exampleInstance);
        for (String exclude : excludeProperty) {
            example.excludeProperty(exclude);
        }
        crit.add(example);
        return crit.list();
    }
    
    /**
     * Use this inside subclasses as a convenience method.
     */
    @SuppressWarnings("unchecked")
    protected List<T> findByCriteria(Criterion... criterion) {
        Criteria crit =
                getSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        return crit.list();
    }
    
    @SuppressWarnings("unchecked")
    public T makePersistent(T entity) {
        return (T) getSession().merge(entity);
    }
    
    public void makeTransient(T entity) {
        getSession().delete(entity);
    }
    
    public void flush() {
        getSession().flush();
    }
    
    public void clear() {
        getSession().clear();
    }
}
