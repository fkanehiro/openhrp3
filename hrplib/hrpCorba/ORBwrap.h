#if defined(MICO)
# include <CORBA.h>
# include <mico/CosNaming.h>
#elif defined(OMNIORB3)
# include <omniORB3/CORBA.h>
#elif defined(OMNIORB4)
# include <omniORB4/CORBA.h>
#elif defined(ORBIXE)
# include <OBE/CORBA.h>
# include <OBE/CosNaming.h>
# include <OBE/OBPolicies.h>
#endif

#define CORBA_Object_var	CORBA::Object_var
#define CORBA_ORB		CORBA::ORB
#define CORBA_ORB_ptr		CORBA::ORB_ptr
#define CORBA_ORB_var		CORBA::ORB_var
#define CORBA_is_nil		CORBA::is_nil
#define CORBA_Boolean		CORBA::Boolean
#define CORBA_Long		CORBA::Long
#define CORBA_Short		CORBA::Short
#define CORBA_ULong		CORBA::ULong
#define CORBA_Float		CORBA::Float
#define CORBA_Double		CORBA::Double
#define CORBA_String_var	CORBA::String_var
#define CosNaming_NamingContext	CosNaming::NamingContext
#define CosNaming_NamingContext_var	CosNaming::NamingContext_var
#define CosNaming_Name		CosNaming::Name
#define CORBA_string_dup	CORBA::string_dup
#define CORBA_SystemException	CORBA::SystemException
#define VERTEX_MAX_SIZE		TriangleContainer::VERTEX_MAX_SIZE
#define TriangleContainerException	TriangleContainer::TriangleContainerException
#define CORBA_ORB_init		CORBA::ORB_init

#ifdef _WIN32
#pragma warning( disable : 4251 4275 4661 )
#endif
