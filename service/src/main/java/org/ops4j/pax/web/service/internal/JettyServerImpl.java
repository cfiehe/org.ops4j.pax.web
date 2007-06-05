package org.ops4j.pax.web.service.internal;

import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.util.LazyList;

public class JettyServerImpl implements JettyServer
{

    private static final Log m_logger = LogFactory.getLog( JettyServer.class );

    private Server m_server;
    private Context m_context;

    public JettyServerImpl()
    {
        m_server = new Server();
    }

    public void start()
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "starting " + this );
        }
        try
        {
            m_server.start();
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "started " + this );
            }
        }
        catch( Exception e )
        {
            if( m_logger.isErrorEnabled() )
            {
                m_logger.error( e );
            }
        }
    }

    public void stop()
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "stopping " + this );
        }
        try
        {
            m_server.stop();
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "stopped " + this );
            }
        }
        catch( Exception e )
        {
            if( m_logger.isErrorEnabled() )
            {
                m_logger.error( e );
            }
        }
    }

    public void addConnector( Connector connector )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "adding connector" + connector );
        }
        m_server.addConnector( connector );
        // TODO handle the case that port is in use. maybe not start the service at all.
    }

    public void addContext( Handler servletHandler )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "adding context");
        }
        m_context = new Context( m_server, "/", Context.SESSIONS );
        m_context.setServletHandler( (ServletHandler) servletHandler );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "added context: " + m_context );
        }
    }

    public String addServlet( final String alias, final Servlet servlet )
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "adding servlet: [" + alias + "] -> " + servlet );
        }
        ServletHolder holder = new ServletHolder( servlet );
        m_context.addServlet( holder, alias + "/*" );
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "added servlet: [" + alias + "] -> " + servlet );
        }
        return holder.getName();
    }

    public void removeServlet( String name )
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "removing servlet: [" + name + "]" );
        }
        // jetty does not provide a method fro removing a servlet so we have to do it by our own
        // the facts bellow are found by analyzing ServletHolder implementation
        boolean removed = false;
        ServletHandler servletHandler = m_context.getServletHandler();
        ServletHolder[] holders = servletHandler.getServlets();
        if ( holders != null )
        {
            ServletHolder holder = servletHandler.getServlet( name );
            if ( holder != null )
            {
                servletHandler.setServlets( (ServletHolder[]) LazyList.removeFromArray( holders, holder ) );
                // we have to find the servlet mapping by hand :( as there is no method provided by jetty
                // and the remove is done based on equals, that is not implemented by servletmapping
                // so it is == based.
                ServletMapping[] mappings = servletHandler.getServletMappings();
                if ( mappings != null )
                {
                    ServletMapping mapping = null;
                    for( ServletMapping item : mappings)
                    {
                        if ( holder.getName().equals( item.getServletName() ) )
                        {
                            mapping = item;
                            break;
                        }
                    }
                    if ( mapping != null )
                    {
                        servletHandler.setServletMappings( (ServletMapping[]) LazyList.removeFromArray( mappings, mapping ) );
                        removed = true;
                    }
                }
            }
        }
        if ( !removed )
        {
            throw new IllegalStateException( name + " was not found" );
        }
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "removed servlet: [" + name + "]" );
        }
    }

}
