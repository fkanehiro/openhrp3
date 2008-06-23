// -*- C++ -*-
#ifndef OPENHRP_EXECUTION_CONTEXT_H_INCLUDED
#define OPENHRP_EXECUTION_CONTEXT_H_INCLUDED

#include <ace/Task.h>
#include <ace/Synch.h>

#include <rtm/RTC.h>
#include <rtm/Manager.h>
#include <rtm/PeriodicExecutionContext.h>
#include <rtm/ECFactory.h>

namespace RTC {

  class OpenHRPExecutionContext : public virtual PeriodicExecutionContext
  {
  public:
    OpenHRPExecutionContext() : PeriodicExecutionContext() { }
    virtual ~OpenHRPExecutionContext() { }
    virtual void tick() throw (CORBA::SystemException) { std::for_each(m_comps.begin(), m_comps.end(), invoke_worker()); }
    virtual int svc(void) { return 0; }
    
    static void init(RTC::Manager* manager)
    {
      manager->registerECFactory("OpenHRPExecutionContext",
				 ECCreate<OpenHRPExecutionContext>,
				 ECDelete<OpenHRPExecutionContext>);
    }
  };
}

#endif
