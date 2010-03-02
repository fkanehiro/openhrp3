all: move_ankle

CXX_FLAGS = `pkg-config --cflags openhrp3.1`
LINK_FLAGS = `pkg-config --libs openhrp3.1`

%.o: %.cpp
	$(CXX) -c $(CXX_FLAGS) -o $@ $<

move_ankle : move_ankle.o
	$(CXX) -o $@ $^ $(LINK_FLAGS)

clean:
	rm *.o move_ankle
