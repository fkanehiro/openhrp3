/**
   @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_UTIL_REFERENCED_H_INCLUDED
#define OPENHRP_UTIL_REFERENCED_H_INCLUDED

#include <boost/intrusive_ptr.hpp>

namespace hrp {
    class Referenced;
    void intrusive_ptr_add_ref(Referenced* obj);
    void intrusive_ptr_release(Referenced* obj);
    
    class Referenced
    {
      public:
        Referenced() { refCounter_ = 0; }
        virtual ~Referenced() { }

    protected:
        int refCounter() { return refCounter_; }

      private:
        friend void intrusive_ptr_add_ref(Referenced* obj);
        friend void intrusive_ptr_release(Referenced* obj);

        int refCounter_;
    };

    inline void intrusive_ptr_add_ref(hrp::Referenced* obj){
        obj->refCounter_++;
    }

    inline void intrusive_ptr_release(hrp::Referenced* obj){
        obj->refCounter_--;
        if(obj->refCounter_ == 0){
            delete obj;
        }
    }
};

#endif
