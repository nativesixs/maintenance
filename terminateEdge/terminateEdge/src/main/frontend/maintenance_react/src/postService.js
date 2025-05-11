const postService = async (requestOptions, link) => {
    try {
      const response = await fetch(link, requestOptions);
      // const responseData = await response.json();
      const contentType = response.headers.get('Content-Type');
      if (contentType && contentType.indexOf('application/json') !== -1) {
          // Handle JSON response
          const responseData = await response.json();
          return responseData;
      } else {
          // Handle non-JSON response (e.g., text, HTML, etc.)
          const responseData = await response.text();
          return responseData;
      }
      // return responseData;
    } catch (error) {
      console.error('Error:', error);
      throw error;
    }
  };
  
  export default postService;