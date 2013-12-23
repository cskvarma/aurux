function Y = spread(X,E)
%  Each point (maxima) in X is "spread" (convolved) with the
%  profile E; Y is the pointwise max of all of these.
%  If E is a scalar, it's the SD of a gaussian used as the
%  spreading function (default 4).
% 2009-03-15 Dan Ellis dpwe@ee.columbia.edu

if nargin < 2; E = 4; end

if length(E) == 1
  W = 4*E;
    E = exp(-0.5*[(-W:W)/E].^2);
    end

    X = locmax(X);
    Y = 0*X;
    lenx = length(X);
    maxi = length(X) + length(E);
    spos = 1+round((length(E)-1)/2);
    for i = find(X>0)
      EE = [zeros(1,i),E];
        EE(maxi) = 0;
	  EE = EE(spos+(1:lenx));
	    Y = max(Y,X(i)*EE);
	    end

