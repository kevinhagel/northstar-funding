"""
NorthStar Funding Intelligence Platform - Main Streamlit Application
===================================================================

Main entry point for the Streamlit admin interface.
Provides navigation and orchestrates the human-AI collaboration workflows.
"""

import streamlit as st
from pathlib import Path

# Configure page
st.set_page_config(
    page_title="NorthStar Funding Intelligence Platform",
    page_icon="⭐",
    layout="wide",
    initial_sidebar_state="expanded"
)

def main():
    """Main application entry point with navigation."""
    
    st.title("⭐ NorthStar Funding Intelligence Platform")
    st.markdown("*Human-AI Collaboration for Funding Discovery and Contact Intelligence*")
    
    # Navigation sidebar
    st.sidebar.title("Navigation")
    
    # Placeholder for navigation - will be implemented in later phases
    page = st.sidebar.radio(
        "Go to:",
        [
            "Dashboard",
            "Discovery Queue", 
            "Enhancement",
            "Approval Workflow",
            "Settings"
        ]
    )
    
    # Main content area
    if page == "Dashboard":
        show_dashboard()
    elif page == "Discovery Queue":
        st.info("📋 Discovery Queue page - To be implemented in Phase 3.10 (T044)")
    elif page == "Enhancement":
        st.info("🔍 Enhancement page - To be implemented in Phase 3.10 (T045)")  
    elif page == "Approval Workflow":
        st.info("✅ Approval Workflow page - To be implemented in Phase 3.10 (T046)")
    elif page == "Settings":
        show_settings()

def show_dashboard():
    """Display the main dashboard."""
    st.header("📊 Dashboard")
    
    # Status indicators
    col1, col2, col3 = st.columns(3)
    
    with col1:
        st.metric("Backend Status", "🟡 Setup Phase", "Phase 3.1")
    
    with col2:
        st.metric("Database", "🟡 Not Connected", "Pending Phase 3.2")
        
    with col3:
        st.metric("AI Services", "🔴 Not Configured", "Pending Phase 3.11")
    
    st.markdown("---")
    
    # Quick stats (placeholder)
    st.subheader("System Overview")
    st.info("""
    **Current Status**: Phase 3.1 - Project Setup
    
    **Completed Setup Tasks:**
    - ✅ Spring Boot 3.5.5 project structure 
    - ✅ PostgreSQL connection configuration
    - ✅ Maven 3.9.9 dependencies
    - ✅ Docker Compose setup
    - ✅ Streamlit project structure
    
    **Next Phase**: 3.2 - Database Schema (TDD Foundation)
    """)

def show_settings():
    """Display system settings and configuration."""
    st.header("⚙️ Settings")
    
    st.subheader("Backend Configuration")
    st.code("""
    Backend URL: http://192.168.1.10:8080 (Mac Studio)
    Database: PostgreSQL on 192.168.1.10:5432
    AI Services: LM Studio on 192.168.1.10:1234
    """)
    
    st.subheader("Development Environment")
    st.code("""
    Frontend: MacBook M2 (192.168.1.5)
    Backend Development: MacBook M2 
    Production Services: Mac Studio (192.168.1.10)
    """)
    
    st.info("Configuration will be finalized in Phase 3.9 - Security & Configuration")

if __name__ == "__main__":
    main()
