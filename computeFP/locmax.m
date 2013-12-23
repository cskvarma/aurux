function Y = locmax(X)
%  Y contains only the points in (vector) X which are local maxima

% Make X a row
X = X(:)';
nbr = [X,X(end)] >= [X(1),X];
% >= makes sure final bin is always zero
Y = X .* nbr(1:end-1) .* (1-nbr(2:end));
